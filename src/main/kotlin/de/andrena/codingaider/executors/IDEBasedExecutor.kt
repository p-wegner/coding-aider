package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import java.awt.EventQueue.invokeLater
import kotlin.concurrent.thread

class IDEBasedExecutor(
    private val project: Project,
    private val commandData: CommandData
) : CommandObserver {
    private val LOG = Logger.getInstance(IDEBasedExecutor::class.java)
    private lateinit var markdownDialog: MarkdownDialog
    private var currentCommitHash: String? = null

    fun execute(): MarkdownDialog {
        markdownDialog = initializeMarkdownDialog()
        currentCommitHash = GitUtils.getCurrentCommitHash(project)

        thread {
            try {
                executeAiderCommand()
            } catch (e: Exception) {
                handleExecutionError(e)
            }
        }
        return markdownDialog
    }

    private fun initializeMarkdownDialog(): MarkdownDialog {
        return MarkdownDialog(project, "Aider Command Output", "Initializing Aider command...").apply {
            isVisible = true
            focus()
        }
    }

    private fun executeAiderCommand() {
        val commandExecutor = CommandExecutor(project, commandData)
        commandExecutor.addObserver(this)
        commandExecutor.executeCommand()
    }

    private fun handleExecutionError(e: Exception) {
        LOG.error("Error executing Aider command", e)
        updateDialogProgress("Error executing Aider command: ${e.message}", "Aider Command Error")
        markdownDialog.startAutoCloseTimer()
    }

    private fun performPostExecutionTasks() {
        markdownDialog.startAutoCloseTimer()
        refreshFiles()
        openGitComparisonToolIfNeeded()
        markdownDialog.focus()
    }

    private fun refreshFiles() {
        val files = commandData.files.mapNotNull {
            VirtualFileManager.getInstance().findFileByUrl(it.filePath)
        }.toTypedArray()
        FileRefresher.refreshFiles(files, markdownDialog)
    }

    private fun openGitComparisonToolIfNeeded() {
        val settings = AiderSettings.getInstance(project)
        if (settings.showGitComparisonTool) {
            currentCommitHash?.let { GitUtils.openGitComparisonTool(project, it) { markdownDialog.focus(1000) } }
        }
    }

    private fun updateDialogProgress(message: String, title: String) {
        invokeLater {
            markdownDialog.updateProgress(message, title)
        }
    }

    override fun onCommandStart(command: String) {
        updateDialogProgress(command, "Aider Command Started")
    }

    override fun onCommandProgress(output: String, runningTime: Long) {
        updateDialogProgress(output, "Aider command in progress ($runningTime seconds)")
    }

    override fun onCommandComplete(output: String, exitCode: Int) {
        val status = if (exitCode == 0) "Completed" else "Failed"
        updateDialogProgress(output, "Aider Command $status")
        performPostExecutionTasks()
    }

    override fun onCommandError(error: String) {
        updateDialogProgress(error, "Aider Command Error")
        markdownDialog.startAutoCloseTimer()
    }
}
