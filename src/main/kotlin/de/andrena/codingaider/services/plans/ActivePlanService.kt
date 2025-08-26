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
    private var currentSubplan: AiderPlan? = null
    private val isContinuing = AtomicBoolean(false)

    fun setActivePlan(plan: AiderPlan) {
        activePlan = plan
        // Reset subplan when setting a new active plan
        currentSubplan = null
    }

    fun getActivePlan(): AiderPlan? = activePlan

    fun setCurrentSubplan(subplan: AiderPlan?) {
        currentSubplan = subplan
    }

    fun getCurrentSubplan(): AiderPlan? = currentSubplan

    /**
     * Gets the next subplan that should be executed based on current progress.
     * Returns null if no subplans are ready or if all subplans are complete.
     */
    fun getNextExecutableSubplan(): AiderPlan? {
        val plan = activePlan ?: return null
        
        // If we have a current subplan that's not complete, continue with it
        currentSubplan?.let { subplan ->
            if (!subplan.isPlanComplete()) {
                return subplan
            }
        }
        
        // Find the first incomplete subplan
        val nextSubplan = plan.childPlans.firstOrNull { !it.isPlanComplete() }
        if (nextSubplan != null) {
            currentSubplan = nextSubplan
            return nextSubplan
        }
        
        // If all subplans are complete but root plan isn't, work on root plan
        return if (!plan.isPlanComplete()) plan else null
    }

    /**
     * Determines if we should execute a specific subplan vs the root plan
     */
    fun shouldExecuteSubplan(): Boolean {
        val plan = activePlan ?: return false
        val executableSubplan = getNextExecutableSubplan()
        
        // Execute subplan if we have one and it's not the root plan
        return executableSubplan != null && executableSubplan != plan
    }

    /**
     * Gets the current subplan execution status for logging and UI purposes
     */
    fun getSubplanExecutionStatus(): String {
        val plan = activePlan ?: return "No active plan"
        
        if (plan.childPlans.isEmpty()) {
            return "Root plan only (no subplans)"
        }
        
        val completedSubplans = plan.childPlans.count { it.isPlanComplete() }
        val totalSubplans = plan.childPlans.size
        val currentSubplan = getCurrentSubplan()
        
        val statusBuilder = StringBuilder()
        statusBuilder.append("Subplans: $completedSubplans/$totalSubplans complete")
        
        if (currentSubplan != null) {
            val subplanName = currentSubplan.mainPlanFile?.filePath?.substringAfterLast('/')?.removeSuffix(".md") ?: "Unknown"
            val openItems = currentSubplan.openChecklistItems().size
            statusBuilder.append(" | Current: $subplanName ($openItems open items)")
        } else {
            statusBuilder.append(" | Working on root plan")
        }
        
        return statusBuilder.toString()
    }

    fun refreshActivePlan() {
        val currentSubplanId = currentSubplan?.id
        
        activePlan = activePlan?.mainPlanFile?.let { mainFile ->
            val file = File(mainFile.filePath)
            // Force filesystem refresh to ensure we see latest changes
            LocalFileSystem.getInstance().findFileByPath(file.absolutePath)?.refresh(false, false)
            project.service<AiderPlanService>().loadPlanFromFile(file)
        }
        
        // Update current subplan reference if we had one
        if (currentSubplanId != null) {
            currentSubplan = activePlan?.getAllChildPlans()?.find { it.id == currentSubplanId }
            
            if (currentSubplan == null && AiderSettings.getInstance().enablePlanCompletionLogging) {
                logger.info("Current subplan reference lost during refresh: $currentSubplanId")
            }
        }
    }

    private fun refreshPlanFiles() {
        activePlan?.let { plan ->
            // Refresh all plan-related files to ensure we see the latest changes
            plan.allFiles.forEach { fileData ->
                LocalFileSystem.getInstance().findFileByPath(fileData.filePath)?.refresh(false, false)
            }
            
            // Also refresh the plan directory itself
            val planDir = File(project.basePath, project.service<AiderPlanService>().getAiderPlansFolder())
            LocalFileSystem.getInstance().findFileByPath(planDir.absolutePath)?.refresh(false, true)
        }
    }

    fun clearActivePlan() {
        activePlan = null
        currentSubplan = null
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
            try {
                // Wait for file system refresh to complete
                Thread.sleep(settings.planCompletionCheckDelay.toLong())
                
                // Force a comprehensive filesystem refresh for all plan files
                refreshPlanFiles()
                
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
        
        // Check for subplan completion and transition
        val currentSubplanComplete = handleSubplanCompletion(currentPlan, settings)
        if (currentSubplanComplete) {
            return // Subplan transition handled, exit early
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

    /**
     * Handles subplan completion and transitions to the next subplan if needed.
     * Returns true if a subplan transition was handled, false otherwise.
     */
    private fun handleSubplanCompletion(currentPlan: AiderPlan, settings: AiderSettings): Boolean {
        val currentSubplan = getCurrentSubplan()
        
        // If we have a current subplan and it's complete, try to move to next subplan
        if (currentSubplan != null && currentSubplan.isPlanComplete()) {
            if (settings.enablePlanCompletionLogging) {
                logger.info("Current subplan complete: ${currentSubplan.id}")
            }
            
            // Find next incomplete subplan
            val nextSubplan = currentPlan.childPlans.firstOrNull { 
                !it.isPlanComplete() && it != currentSubplan 
            }
            
            if (nextSubplan != null) {
                if (settings.enablePlanCompletionLogging) {
                    logger.info("Transitioning to next subplan: ${nextSubplan.id}")
                }
                setCurrentSubplan(nextSubplan)
                
                if (settings.enableAutoPlanContinue) {
                    continuePlan()
                }
                return true // Subplan transition handled
            } else {
                // All subplans complete, clear current subplan and continue with root plan
                if (settings.enablePlanCompletionLogging) {
                    logger.info("All subplans complete, continuing with root plan")
                }
                setCurrentSubplan(null)
                
                if (!currentPlan.isPlanComplete() && settings.enableAutoPlanContinue) {
                    continuePlan()
                }
                return true // Subplan transition handled
            }
        }
        
        return false // No subplan transition needed
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

    /**
     * Collects virtual files for execution based on current plan state.
     * Used by tests and execution logic to determine which files to include.
     */
    fun collectVirtualFilesForExecution(): List<FileData> {
        val plan = activePlan ?: return emptyList()
        val planToExecute = getEffectivePlanForExecution(plan)
        
        val filesToInclude = mutableSetOf<FileData>()
        
        if (planToExecute != plan) {
            // Executing a subplan - include root plan files + current subplan files
            filesToInclude.addAll(plan.planFiles)  // Always include root plan files
            filesToInclude.addAll(planToExecute.allFiles)  // Include subplan files
        } else {
            // Executing root plan - include all files
            filesToInclude.addAll(planToExecute.allFiles)
        }
        
        return filesToInclude.toList()
    }

    /**
     * Refreshes the plan state by reloading from filesystem.
     * Used by tests to simulate plan state changes.
     */
    fun refreshPlanState() {
        refreshActivePlan()
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

        // Force refresh to ensure we have the latest plan state
        refreshActivePlan()
        val refreshedPlan = activePlan ?: throw IllegalStateException("Active plan was lost during refresh")

        if (refreshedPlan.isPlanComplete()) {
            // Check for uncompleted child or sibling plans
            val nextPlans = refreshedPlan.getNextUncompletedPlansInSameFamily()
            if (nextPlans.isNotEmpty()) {
                if (AiderSettings.getInstance().enablePlanCompletionLogging) {
                    logger.info("Current plan complete, switching to next plan: ${nextPlans.first().id}")
                }
                setActivePlan(nextPlans.first()) // Set the first uncompleted plan as active
            } else {
                if (AiderSettings.getInstance().enablePlanCompletionLogging) {
                    logger.info("All plans in hierarchy complete, clearing active plan")
                }
                clearActivePlan()
                throw IllegalStateException("All plans in hierarchy are complete - no further actions needed")
            }
        }

        val currentActivePlan = activePlan ?: throw IllegalStateException("No active plan after validation")
        
        if (currentActivePlan.checklist.isEmpty()) {
            throw IllegalStateException("Plan has no checklist items")
        }

        // Validate subplan state if we have subplans
        val executableSubplan = getNextExecutableSubplan()
        if (executableSubplan != null && executableSubplan != currentActivePlan) {
            // We have a subplan to execute - validate its state
            if (executableSubplan.checklist.isEmpty()) {
                throw IllegalStateException("Subplan ${executableSubplan.id} has no checklist items")
            }
            
            val subplanOpenItems = executableSubplan.openChecklistItems()
            if (subplanOpenItems.isEmpty()) {
                throw IllegalStateException("Subplan ${executableSubplan.id} has no open items but is marked as incomplete")
            }
            
            if (AiderSettings.getInstance().enablePlanCompletionLogging) {
                logger.info("Validated subplan: ${executableSubplan.id} with ${subplanOpenItems.size} open items")
            }
        } else {
            // Validate root plan state
            val openItems = currentActivePlan.openChecklistItems()
            if (openItems.isEmpty()) {
                // If current plan has no open items but isn't complete, it might have uncompleted child plans
                if (!currentActivePlan.isPlanComplete()) {
                    val nextPlan = currentActivePlan.getNextUncompletedPlansInSameFamily().firstOrNull()
                    if (nextPlan != null) {
                        if (AiderSettings.getInstance().enablePlanCompletionLogging) {
                            logger.info("No open items in current plan, switching to next plan: ${nextPlan.id}")
                        }
                        setActivePlan(nextPlan)
                    } else {
                        throw IllegalStateException("Inconsistent plan state: no open items but plan is not complete")
                    }
                } else {
                    throw IllegalStateException("No open items found in checklist. The plan may need to be updated.")
                }
            }
        }
    }

    private fun executePlanContinuation(selectedPlan: AiderPlan) {
        try {
            validatePlanFiles(selectedPlan)

            val fileSystem = LocalFileSystem.getInstance()
            val settings = AiderSettings.getInstance()
            val projectBasePath = project.basePath ?: throw IllegalStateException("Project base path not found")

            // Determine which plan to execute and collect appropriate files
            val planToExecute = getEffectivePlanForExecution(selectedPlan)
            
            // Set the current subplan for prompt generation if we're executing a subplan
            if (planToExecute != selectedPlan) {
                setCurrentSubplan(planToExecute)
            } else {
                setCurrentSubplan(null)  // Clear subplan when executing root plan
            }
            
            val virtualFiles = collectVirtualFilesForExecution(planToExecute, fileSystem, projectBasePath)
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
                planId = planToExecute.mainPlanFile?.filePath ?: selectedPlan.mainPlanFile?.filePath
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

    /**
     * Determines which plan should actually be executed (root plan or a specific subplan)
     */
    private fun getEffectivePlanForExecution(rootPlan: AiderPlan): AiderPlan {
        // Check if we should execute a specific subplan
        val executableSubplan = getNextExecutableSubplan()
        
        if (executableSubplan != null && executableSubplan != rootPlan) {
            // We have a specific subplan to execute
            if (AiderSettings.getInstance().enablePlanCompletionLogging) {
                logger.info("Executing subplan: ${executableSubplan.id}")
            }
            return executableSubplan
        }
        
        // Execute root plan
        if (AiderSettings.getInstance().enablePlanCompletionLogging) {
            logger.info("Executing root plan: ${rootPlan.id}")
        }
        return rootPlan
    }

    /**
     * Collects files for execution based on whether we're executing a subplan or root plan
     */
    private fun collectVirtualFilesForExecution(
        planToExecute: AiderPlan,
        fileSystem: LocalFileSystem,
        projectBasePath: String
    ): List<VirtualFile> {
        val rootPlan = activePlan
        val filesToInclude = mutableSetOf<FileData>()
        
        if (planToExecute != rootPlan && rootPlan != null) {
            // Executing a subplan - include root plan files + current subplan files
            filesToInclude.addAll(rootPlan.planFiles)  // Always include root plan files
            filesToInclude.addAll(planToExecute.allFiles)  // Include subplan files
            
            if (AiderSettings.getInstance().enablePlanCompletionLogging) {
                logger.info("Subplan execution - including ${rootPlan.planFiles.size} root plan files and ${planToExecute.allFiles.size} subplan files")
            }
        } else {
            // Executing root plan - include all files
            filesToInclude.addAll(planToExecute.allFiles)
        }
        
        val virtualFiles = filesToInclude.mapNotNull { fileData ->
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
