package de.andrena.codingaider.services.plans

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.services.sidecar.AiderProcessManager
import de.andrena.codingaider.settings.AiderSettings
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class ActivePlanService(private val project: Project) {
    private val logger = Logger.getInstance(ActivePlanService::class.java)
    private var activePlan: AiderPlan? = null
    private val isContinuing = AtomicBoolean(false)

    fun setActivePlan(plan: AiderPlan) {
        activePlan = plan
    }

    fun getActivePlan(): AiderPlan? = activePlan

    fun refreshActivePlan() {
        activePlan = activePlan?.mainPlanFile?.let { mainFile ->
            project.service<AiderPlanService>().loadPlanFromFile(File(mainFile.filePath))
        }
    }

    fun clearActivePlan() {
        activePlan = null
    }

    private fun ChecklistItem.isComplete(): Boolean {
        return checked && children.all { it.isComplete() }
    }

    fun handlePlanActionFinished(success: Boolean = true) {
        if (!success) {
            cleanupAndClearPlan()
            return
        }

        val settings = AiderSettings.getInstance()
        if (settings.enablePlanCompletionLogging) {
            logger.info("Plan action finished, starting completion check with delay: ${settings.planCompletionCheckDelay}ms")
        }

        // Add proper timing with delayed execution to ensure file system operations complete
        ApplicationManager.getApplication().invokeLater {
            // Wait for file system refresh to complete
            Thread.sleep(settings.planCompletionCheckDelay.toLong())
            
            try {
                refreshActivePlan()
                checkAndContinuePlanIfEnabled()
            } catch (e: Exception) {
                logger.error("Error during plan completion check", e)
                handlePlanError(e)
            }
        }
    }

    private fun checkAndContinuePlanIfEnabled() {
        val currentPlan = activePlan ?: return
        val settings = AiderSettings.getInstance()
        
        if (settings.enablePlanCompletionLogging) {
            logger.info("Checking plan completion for plan: ${currentPlan.id}, isPlanComplete: ${currentPlan.isPlanComplete()}")
        }
        
        if (!currentPlan.isPlanComplete()) {
            if (settings.enableAutoPlanContinue) {
                if (settings.enablePlanCompletionLogging) {
                    logger.info("Plan not complete, continuing current plan: ${currentPlan.id}")
                }
                continuePlan()
                return
            }
        }
        
        // Try to find next uncompleted plan in hierarchy
        val nextPlans = currentPlan.getNextUncompletedPlansInSameFamily()
        if (nextPlans.isNotEmpty()) {
            if (settings.enablePlanCompletionLogging) {
                logger.info("Current plan complete, found ${nextPlans.size} uncompleted plans in family")
            }
            cleanupAndClearPlan() // Cleanup current plan's resources
            setActivePlan(nextPlans.first()) // Set the first uncompleted plan as active

            if (settings.enableAutoPlanContinuationInPlanFamily) {
                if (settings.enablePlanCompletionLogging) {
                    logger.info("Continuing with next plan in family: ${nextPlans.first().id}")
                }
                continuePlan()
            }
        } else {
            if (settings.enablePlanCompletionLogging) {
                logger.info("All plans in hierarchy complete, cleaning up")
            }
            cleanupAndClearPlan()
        }
    }

    private fun cleanupAndClearPlan() {
        activePlan?.let { plan ->
            // Clean up sidecar process if enabled
            if (AiderSettings.getInstance().useSidecarMode) {
                try {
                    plan.mainPlanFile?.filePath?.let { planPath ->
                        project.service<AiderProcessManager>().disposePlanProcess(plan.id)
                    }
                } catch (e: Exception) {
                    logger.error("Error disposing plan process: ${e.message}", e)
                }
            }
        }
        clearActivePlan()
    }

    fun handlePlanError(error: Throwable? = null) {
        error?.let {
            println("Plan execution error: ${it.message}")
            it.printStackTrace()
        }
        clearActivePlan()
    }

    fun continuePlan() {
        // Prevent multiple simultaneous continuations
        if (!isContinuing.compareAndSet(false, true)) {
            logger.info("Plan continuation already in progress, skipping manual continuation")
            return
        }

        try {
            continuePlanInternal()
        } finally {
            isContinuing.set(false)
        }
    }

    private fun continuePlanInternal() {
        try {
            validatePlanState()
            val plan = activePlan ?: throw IllegalStateException("No active plan found to continue")
            executePlanContinuation(plan)
        } catch (e: Exception) {
            handlePlanError(e)
            when (e) {
                is IllegalStateException -> throw e
                is SecurityException -> throw IllegalStateException(
                    "Security error during plan continuation: ${e.message}",
                    e
                )

                else -> throw IllegalStateException("Unexpected error during plan continuation: ${e.message}", e)
            }
        }
    }

    private fun validatePlanState() {
        val plan = activePlan ?: throw IllegalStateException("No active plan found to continue")

        if (plan.isPlanComplete()) {
            // Check for uncompleted child or sibling plans
            val nextPlans = plan.getNextUncompletedPlansInSameFamily()
            if (nextPlans.isNotEmpty()) {
                setActivePlan(nextPlans.first()) // Set the first uncompleted plan as active
            } else {
                clearActivePlan()
                throw IllegalStateException("All plans in hierarchy are complete - no further actions needed")
            }
        }

        if (plan.checklist.isEmpty()) {
            throw IllegalStateException("Plan has no checklist items")
        }

        val openItems = plan.openChecklistItems()
        if (openItems.isEmpty()) {
            // If current plan has no open items but isn't complete, it might have uncompleted child plans
            if (!plan.isPlanComplete()) {
                val nextPlan = plan.getNextUncompletedPlansInSameFamily().firstOrNull()
                if (nextPlan != null) {
                    setActivePlan(nextPlan)
                } else {
                    throw IllegalStateException("Inconsistent plan state: no open items but plan is not complete")
                }
            } else {
                throw IllegalStateException("No open items found in checklist. The plan may need to be updated.")
            }
        }
    }

    private fun executePlanContinuation(selectedPlan: AiderPlan) {
        try {
            validatePlanFiles(selectedPlan)

            val fileSystem = LocalFileSystem.getInstance()
            val settings = AiderSettings.getInstance()
            val projectBasePath = project.basePath ?: throw IllegalStateException("Project base path not found")

            // Validate and collect files
            val virtualFiles = collectVirtualFiles(selectedPlan, fileSystem, projectBasePath)
            val filesToInclude = collectFilesToInclude(virtualFiles)

            // Create command data with plan-specific settings
            val commandData = CommandData(
                message = "",
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = filesToInclude,
                lintCmd = settings.lintCmd,
                projectPath = projectBasePath,
                aiderMode = AiderMode.STRUCTURED,
                sidecarMode = settings.useSidecarMode,
                planId = selectedPlan.mainPlanFile?.filePath
            )
            setActivePlan(selectedPlan)
            IDEBasedExecutor(project, commandData, { handlePlanActionFinished(it) }).execute()

        } catch (e: Exception) {
            val errorMessage = when (e) {
                is IllegalStateException -> "Plan continuation failed: ${e.message}"
                is SecurityException -> "Security error during plan continuation: ${e.message}"
                else -> "Unexpected error during plan continuation: ${e.message}"
            }
            println(errorMessage)
            clearActivePlan()
            throw IllegalStateException(errorMessage, e)
        }
    }

    private fun validatePlanFiles(plan: AiderPlan) {
        if (plan.planFiles.isEmpty()) {
            throw IllegalStateException("Plan has no associated files")
        }

        val invalidFiles = plan.planFiles.filter { !File(it.filePath).exists() }
        if (invalidFiles.isNotEmpty()) {
            throw IllegalStateException("Some plan files are missing or inaccessible: ${invalidFiles.joinToString { it.filePath }}")
        }
    }

    private fun collectVirtualFiles(
        plan: AiderPlan,
        fileSystem: LocalFileSystem,
        projectBasePath: String
    ): List<VirtualFile> {
        val virtualFiles = plan.allFiles.mapNotNull { fileData ->
            fileSystem.findFileByPath(fileData.filePath)
                ?: fileSystem.findFileByPath("$projectBasePath/${fileData.filePath}")
                ?: run {
                    println("Warning: Could not find file: ${fileData.filePath}")
                    null
                }
        }

        if (virtualFiles.isEmpty()) {
            throw IllegalStateException("No valid files found for plan continuation. Check if files exist and are accessible.")
        }
        return virtualFiles
    }

    private fun collectFilesToInclude(virtualFiles: List<VirtualFile>): List<FileData> {
        val filesToInclude = project.service<FileDataCollectionService>()
            .collectAllFiles(virtualFiles.toTypedArray())

        if (filesToInclude.isEmpty()) {
            throw IllegalStateException("No files could be collected for plan continuation. Check file permissions and accessibility.")
        }
        return filesToInclude
    }

}
