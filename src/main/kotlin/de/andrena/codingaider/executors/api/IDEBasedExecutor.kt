package de.andrena.codingaider.executors.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.CommandExecutor
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.CodingAiderOutputPresentation
import de.andrena.codingaider.outputview.AiderOutputTab
import de.andrena.codingaider.services.AiderOutputService
import de.andrena.codingaider.services.CommandSummaryService
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.PlanExecutionCostService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class IDEBasedExecutor(
    private val project: Project,
    private val commandData: CommandData,
    private val commandFinishedCallback: CommandFinishedCallback? = null
) : CommandObserver, Abortable {
    private val log = Logger.getInstance(IDEBasedExecutor::class.java)
    private val planExecutionActions = CommandPlanExecutionHandler(project, commandData)
    private var outputDisplay: CodingAiderOutputPresentation? = null // AiderOutputTab
    private var currentCommitHash: String? = commandData.options.commitHashToCompareWith
    private val commandExecutor = AtomicReference<CommandExecutor?>(null)
    private var executionThread: Thread? = null
    private var isFinished: CountDownLatch = CountDownLatch(1)
    private var finalOutput: String = ""

    fun getFinalOutput(): String {
        return finalOutput
    }

    fun execute(): Any {
        val outputService = project.service<AiderOutputService>()
        
        // Create output with immediate feedback
        val initialMessage = buildInitialMessage()
        outputDisplay = outputService.createOutput(
            "Aider Command Output",
            initialMessage,
            this,
            project.service<CommandSummaryService>().generateSummary(commandData),
            commandData
        )
        
        // Add to running commands immediately
        if (outputDisplay is AiderOutputTab) {
            project.service<RunningCommandService>().addRunningCommand(outputDisplay as AiderOutputTab)
        }
        
        // Start execution immediately in background
        executionThread = thread {
            try {
                // Update with preparation status
                updateOutputProgress("Preparing Aider command...", "Aider Command Preparation")
                
                if (currentCommitHash == null) {
                    currentCommitHash = GitUtils.getCurrentCommitHash(project)
                }

                planExecutionActions.beforeCommandStarted()
                
                executeAiderCommand()
            } finally {
                isFinished.countDown()
            }
        }
        return outputDisplay!!
    }
    
    private fun buildInitialMessage(): String {
        val summary = project.service<CommandSummaryService>().generateSummary(commandData)
        return """
# Aider Command Starting

**Command:** $summary

**Status:** Preparing command execution...

**Files:** ${commandData.files.size} file(s) selected
${if (commandData.files.isNotEmpty()) "- " + commandData.files.take(5).joinToString("\n- ") { it.filePath } else ""}
${if (commandData.files.size > 5) "\n- ... and ${commandData.files.size - 5} more files" else ""}

**Mode:** ${commandData.aiderMode}
${if (commandData.llm.isNotEmpty()) "**LLM:** ${commandData.llm}" else ""}

---

*Command will start shortly...*
        """.trimIndent()
    }

    fun isFinished(): CountDownLatch = isFinished

    private fun executeAiderCommand() {
        try {
            val executor = CommandExecutor(commandData, project).apply {
                addObserver(this@IDEBasedExecutor)
            }
            commandExecutor.set(executor)
            executor.executeCommand()
        } catch (e: Exception) {
            log.error("Error executing Aider command", e)
            updateOutputProgress("Error executing Aider command: ${e.message}", "Aider Command Error")
        }
    }

    override fun abortCommand(planId: String?) {
        try {
            commandExecutor.get()?.abortCommand(planId)
            executionThread?.interrupt()
            updateOutputProgress("Aider command aborted by user", "Aider Command Aborted")
            val outputService = project.service<AiderOutputService>()
            outputDisplay?.let { outputService.setProcessFinished(it) }
            
            // Remove from running commands when aborted
            if (outputDisplay is AiderOutputTab) {
                project.service<RunningCommandService>().removeRunningCommand(outputDisplay as AiderOutputTab)
            }
            
            Thread.sleep(500)
            ApplicationManager.getApplication().invokeLater {
                if (outputDisplay is AiderOutputTab) {
                    (outputDisplay as AiderOutputTab).dispose()
                }
            }
            isFinished.countDown()
        } catch (e: Exception) {
            log.error("Error during abort", e)
            ApplicationManager.getApplication().invokeLater {
                if (outputDisplay is AiderOutputTab) {
                    (outputDisplay as AiderOutputTab).dispose()
                }
            }
        }
    }

    private fun refreshFiles() {
        val files =
            commandData.files.mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it.filePath) }.toTypedArray()
        FileRefresher.refreshFiles(files, outputDisplay)
    }

    private fun openGitComparisonToolIfNeeded() {
        if (getInstance().showGitComparisonTool) {
            currentCommitHash?.let { 
                GitUtils.openGitComparisonTool(project, it) { 
                    val outputService = project.service<AiderOutputService>()
                    outputDisplay?.let { outputService.focus(it, 1000) }
                } 
            }
        }
    }

    private fun updateOutputProgress(message: String, title: String) {
        ApplicationManager.getApplication().invokeLater {
            if (outputDisplay == null) return@invokeLater
            val outputService = project.service<AiderOutputService>()
            outputService.updateProgress(outputDisplay!!, message, title)
        }
    }

    override fun onCommandStart(message: String) {
        val enhancedMessage = """
# Aider Command Started

${message.trimStart()}

---

*Command is now running...*
        """.trimIndent()
        updateOutputProgress(enhancedMessage, "Aider Command Started")
    }

    override fun onCommandProgress(message: String, runningTime: Long) {
        val enhancedMessage = """
# Aider Command In Progress

**Running Time:** $runningTime seconds

${message.trimStart()}
        """.trimIndent()
        updateOutputProgress(enhancedMessage, "Aider command in progress ($runningTime seconds)")
    }

    override fun onCommandComplete(message: String, exitCode: Int) {
        updateOutputProgress(message, "Aider Command ${if (exitCode == 0) "Completed" else "Failed"}")
        val outputService = project.service<AiderOutputService>()
        outputDisplay?.let { 
            outputService.startAutoCloseTimer(it, commandData.options.autoCloseDelay ?: 10)
        }
        refreshFiles()
        planExecutionActions.commandCompleted()
        
        // Remove from running commands
        if (outputDisplay is AiderOutputTab) {
            project.service<RunningCommandService>().removeRunningCommand(outputDisplay as AiderOutputTab)
        }
        
        // Record execution cost if this is a plan execution
        if (commandData.planId != null && exitCode == 0) {
            try {
                val planService = project.service<AiderPlanService>()
                val costService = service<PlanExecutionCostService>()
                
                // Find the plan by ID
                val plans = planService.getAiderPlans()
                val plan = plans.flatMap { it.getAllChildPlans() + it }
                    .find { it.id == commandData.planId }
                
                // Record execution cost if plan found
                plan?.let {
                    costService.recordExecutionCost(it, message, commandData)
                }
            } catch (e: Exception) {
                log.warn("Failed to record plan execution cost", e)
            }
        }
        
        if (!commandData.options.disablePresentation) {
            presentChanges()
        }
        try {
            val commitBefore = currentCommitHash
            val commitAfter = GitUtils.getCurrentCommitHash(project)
            project.service<RunningCommandService>().storeCompletedCommand(commandData, message, commitBefore, commitAfter)
        } catch (e: InterruptedException) {
            // aborting commands should be handled gracefully
        }
        finalOutput = message
        commandFinishedCallback?.onCommandFinished(exitCode == 0)
    }

    private fun presentChanges() {
        openGitComparisonToolIfNeeded()
        val outputService = project.service<AiderOutputService>()
        outputDisplay?.let { 
            outputService.setProcessFinished(it)
            outputService.focus(it)
        }
    }

    override fun onCommandError(message: String) {
        updateOutputProgress(message, "Aider Command Error")
        val outputService = project.service<AiderOutputService>()
        outputDisplay?.let { 
            outputService.setProcessFinished(it)
            outputService.startAutoCloseTimer(it, 10)
        }
        
        // Remove from running commands on error as well
        if (outputDisplay is AiderOutputTab) {
            project.service<RunningCommandService>().removeRunningCommand(outputDisplay as AiderOutputTab)
        }
    }

}
