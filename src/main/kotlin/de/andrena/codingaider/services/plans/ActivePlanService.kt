package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.CommandFinishedCallback
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings
import java.io.File

@Service(Service.Level.PROJECT)
class ActivePlanService(private val project: Project) {
    private var activePlan: AiderPlan? = null

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

    fun handlePlanCompletion(success: Boolean = true) {
        refreshActivePlan()
        
        if (!success) {
            clearActivePlan()
            return
        }

        val currentPlan = activePlan
        if (currentPlan == null) {
            return
        }

        if (currentPlan.isPlanComplete()) {
            // Try to find next uncompleted plan in hierarchy
            val nextPlans = currentPlan.getNextUncompletedPlan()
            if (nextPlans.isNotEmpty()) {
                setActivePlan(nextPlans.first()) // Set the first uncompleted plan as active
            } else {
                clearActivePlan()
            }
        }
    }

    fun handlePlanError(error: Throwable? = null) {
        error?.let {
            println("Plan execution error: ${it.message}")
            it.printStackTrace()
        }
        clearActivePlan()
    }

    fun continuePlan() {
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
            val nextPlans = plan.getNextUncompletedPlan()
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
                val nextPlan = plan.getNextUncompletedPlan()
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

            val commandData = CommandData(
                message = "",
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = filesToInclude,
                lintCmd = settings.lintCmd,
                projectPath = projectBasePath,
                aiderMode = AiderMode.STRUCTURED,
                sidecarMode = settings.useSidecarMode
            )

            setActivePlan(selectedPlan)
            val commandFinishedCallback: CommandFinishedCallback = object : CommandFinishedCallback {
                override fun onCommandFinished(success: Boolean) = handlePlanCompletion(success)
            }
            IDEBasedExecutor(project, commandData, commandFinishedCallback).execute()

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
