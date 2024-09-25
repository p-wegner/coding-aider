package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import java.awt.EventQueue.invokeLater
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class IDEBasedExecutor(
    private val project: Project,
    private val commandData: CommandData
) : CommandObserver, Abortable {
    private val log = Logger.getInstance(IDEBasedExecutor::class.java)
    private lateinit var markdownDialog: MarkdownDialog
    private var currentCommitHash: String? = null
    private val commandExecutor = AtomicReference<CommandExecutor?>(null)
    private var executionThread: Thread? = null

    fun execute(): MarkdownDialog {
        markdownDialog = MarkdownDialog(
            project,
            "Aider Command Output",
            "Initializing Aider command...",
            this
        )
            .apply {
                isVisible = true
                focus()
            }
        currentCommitHash = GitUtils.getCurrentCommitHash(project)

        executionThread = thread { executeAiderCommand() }
        return markdownDialog
    }

    private fun executeAiderCommand() {
        try {
            val executor = CommandExecutor(commandData).apply {
                addObserver(this@IDEBasedExecutor)
            }
            commandExecutor.set(executor)
            executor.executeCommand()
        } catch (e: Exception) {
            log.error("Error executing Aider command", e)
            updateDialogProgress("Error executing Aider command: ${e.message}", "Aider Command Error")
        }
    }

    override fun abortCommand() {
        commandExecutor.get()?.abortCommand()
        executionThread?.interrupt()
        updateDialogProgress("Aider command aborted by user", "Aider Command Aborted")
        markdownDialog.dispose()
    }

    private fun refreshFiles() {
        val files =
            commandData.files.mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it.filePath) }.toTypedArray()
        FileRefresher.refreshFiles(files, markdownDialog)
    }

    private fun openGitComparisonToolIfNeeded() {
        if (getInstance().showGitComparisonTool) {
            currentCommitHash?.let { GitUtils.openGitComparisonTool(project, it) { markdownDialog.focus(1000) } }
        }
    }

    private fun updateDialogProgress(message: String, title: String) {
        invokeLater { markdownDialog.updateProgress(message, title) }
    }

    override fun onCommandStart(command: String) =
        updateDialogProgress(command, "Aider Command Started")

    override fun onCommandProgress(output: String, runningTime: Long) =
        updateDialogProgress(output, "Aider command in progress ($runningTime seconds)")

    override fun onCommandComplete(output: String, exitCode: Int) {
        updateDialogProgress(output, "Aider Command ${if (exitCode == 0) "Completed" else "Failed"}")
        markdownDialog.startAutoCloseTimer()
        refreshFiles()
        openGitComparisonToolIfNeeded()
        if (!getInstance().closeOutputDialogImmediately) {
            markdownDialog.setProcessFinished()
            markdownDialog.focus()
        }
    }

    override fun onCommandError(error: String) {
        updateDialogProgress(error, "Aider Command Error")
        markdownDialog.setProcessFinished()
        markdownDialog.startAutoCloseTimer()
    }
}
