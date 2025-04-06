package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.CommandExecutor
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.services.CommandSummaryService
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import com.intellij.openapi.wm.WindowManager
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
    private var markdownDialog: MarkdownDialog? = null
    private var currentCommitHash: String? = commandData.options.commitHashToCompareWith
    private val commandExecutor = AtomicReference<CommandExecutor?>(null)
    private var executionThread: Thread? = null
    private var isFinished: CountDownLatch = CountDownLatch(1)


    fun execute(): MarkdownDialog {
        markdownDialog = MarkdownDialog(
            project,
            "Aider Command Output",
            "Initializing Aider command...",
            this,
            displayString = project.service<CommandSummaryService>().generateSummary(commandData),
            commandData = commandData
        ).apply {
            isVisible = true
            focus()
            updateProgress("Initializing Aider command...", "Aider Command Starting")
        }
        if (currentCommitHash == null) {
            currentCommitHash = GitUtils.getCurrentCommitHash(project)
        }


        planExecutionActions.beforeCommandStarted()
        project.service<RunningCommandService>().addRunningCommand(markdownDialog!!)
        executionThread = thread {
            executeAiderCommand()
            isFinished.countDown()
        }
        return markdownDialog!!
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
            updateDialogProgress("Error executing Aider command: ${e.message}", "Aider Command Error")
        }
    }

    override fun abortCommand(planId: String?) {
        try {
            commandExecutor.get()?.abortCommand(planId)
            executionThread?.interrupt()
            updateDialogProgress("Aider command aborted by user", "Aider Command Aborted")
            markdownDialog?.setProcessFinished()
            // Give a small delay to ensure the message is shown before closing
            Thread.sleep(500)
            invokeLater {
                markdownDialog?.dispose()
            }
            isFinished.countDown()
        } catch (e: Exception) {
            log.error("Error during abort", e)
            // Ensure dialog is closed even if there's an error
            invokeLater {
                markdownDialog?.dispose()
            }
        }
    }

    private fun refreshFiles() {
        val files =
            commandData.files.mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it.filePath) }.toTypedArray()
        FileRefresher.refreshFiles(files, markdownDialog)
    }

    private fun openGitComparisonToolIfNeeded() {
        if (getInstance().showGitComparisonTool) {
            currentCommitHash?.let { GitUtils.openGitComparisonTool(project, it) { markdownDialog?.focus(1000) } }
        }
    }

    private fun updateDialogProgress(message: String, title: String) {
        invokeLater {
            if (markdownDialog == null) return@invokeLater;
            markdownDialog?.updateProgress(message, title)
        }
    }

    override fun onCommandStart(message: String) =
        updateDialogProgress(message, "Aider Command Started")

    override fun onCommandProgress(message: String, runningTime: Long) =
        updateDialogProgress(message, "Aider command in progress ($runningTime seconds)")

    override fun onCommandComplete(message: String, exitCode: Int) {
        updateDialogProgress(message, "Aider Command ${if (exitCode == 0) "Completed" else "Failed"}")
        markdownDialog?.startAutoCloseTimer(
            commandData.options.autoCloseDelay ?: getInstance().markdownDialogAutocloseDelay
        )
        refreshFiles()
        planExecutionActions.commandCompleted(message)
        // Store the command data and output for potential plan creation
        project.service<RunningCommandService>().storeCompletedCommand(commandData, message)
        if (!commandData.options.disablePresentation) {
            presentChanges()
        }
        commandFinishedCallback?.onCommandFinished(exitCode == 0)
    }

    private fun presentChanges() {
        openGitComparisonToolIfNeeded()
        if (!getInstance().closeOutputDialogImmediately) {
            markdownDialog?.setProcessFinished()
            markdownDialog?.focus()
        }
    }

    override fun onCommandError(message: String) {
        updateDialogProgress(message, "Aider Command Error")
        markdownDialog?.setProcessFinished()
        markdownDialog?.startAutoCloseTimer(getInstance().markdownDialogAutocloseDelay)
    }
    
    companion object {
        @FunctionalInterface
        fun interface CommandFinishedCallback {
            fun onCommandFinished(success: Boolean)
        }
    }
}
