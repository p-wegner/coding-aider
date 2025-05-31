package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.CommandExecutor
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.CodingAiderOutputPresentation
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.services.AiderOutputService
import de.andrena.codingaider.services.CommandSummaryService
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.PlanExecutionCostService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import java.awt.EventQueue.invokeLater
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
    private var outputDisplay: CodingAiderOutputPresentation? = null // Can be MarkdownDialog or AiderOutputTab
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
        outputDisplay = outputService.createOutput(
            "Aider Command Output",
            "Initializing Aider command...",
            this,
            project.service<CommandSummaryService>().generateSummary(commandData),
            commandData
        )
        
        outputService.updateProgress(outputDisplay!!, "Initializing Aider command...", "Aider Command Starting")
        
        if (currentCommitHash == null) {
            currentCommitHash = GitUtils.getCurrentCommitHash(project)
        }

        planExecutionActions.beforeCommandStarted()
        
        // Add to running commands (handle both dialog and tab types)
        if (outputDisplay is MarkdownDialog) {
            project.service<RunningCommandService>().addRunningCommand(outputDisplay as MarkdownDialog)
        }
        
        executionThread = thread {
            executeAiderCommand()
            isFinished.countDown()
        }
        return outputDisplay!!
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
            Thread.sleep(500)
            invokeLater {
                if (outputDisplay is MarkdownDialog) {
                    (outputDisplay as MarkdownDialog).dispose()
                }
            }
            isFinished.countDown()
        } catch (e: Exception) {
            log.error("Error during abort", e)
            invokeLater {
                if (outputDisplay is MarkdownDialog) {
                    (outputDisplay as MarkdownDialog).dispose()
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
        invokeLater {
            if (outputDisplay == null) return@invokeLater
            val outputService = project.service<AiderOutputService>()
            outputService.updateProgress(outputDisplay!!, message, title)
        }
    }

    override fun onCommandStart(message: String) =
        updateOutputProgress(message, "Aider Command Started")

    override fun onCommandProgress(message: String, runningTime: Long) =
        updateOutputProgress(message, "Aider command in progress ($runningTime seconds)")

    override fun onCommandComplete(message: String, exitCode: Int) {
        updateOutputProgress(message, "Aider Command ${if (exitCode == 0) "Completed" else "Failed"}")
        val outputService = project.service<AiderOutputService>()
        outputDisplay?.let { 
            outputService.startAutoCloseTimer(it, commandData.options.autoCloseDelay ?: getInstance().markdownDialogAutocloseDelay)
        }
        refreshFiles()
        planExecutionActions.commandCompleted()
        project.service<RunningCommandService>().storeCompletedCommand(commandData, message)
        
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
        val commitBefore = currentCommitHash
        val commitAfter = GitUtils.getCurrentCommitHash(project)
        project.service<RunningCommandService>().storeCompletedCommand(commandData, message, commitBefore, commitAfter)
        finalOutput = message
        commandFinishedCallback?.onCommandFinished(exitCode == 0)
    }

    private fun presentChanges() {
        openGitComparisonToolIfNeeded()
        if (!getInstance().closeOutputDialogImmediately) {
            val outputService = project.service<AiderOutputService>()
            outputDisplay?.let { 
                outputService.setProcessFinished(it)
                outputService.focus(it)
            }
        }
    }

    override fun onCommandError(message: String) {
        updateOutputProgress(message, "Aider Command Error")
        val outputService = project.service<AiderOutputService>()
        outputDisplay?.let { 
            outputService.setProcessFinished(it)
            outputService.startAutoCloseTimer(it, getInstance().markdownDialogAutocloseDelay)
        }
    }

}
