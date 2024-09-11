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
) {
    private val LOG = Logger.getInstance(IDEBasedExecutor::class.java)

    fun execute(): MarkdownDialog {
        val markdownDialog = initializeMarkdownDialog()
        val currentCommitHash = GitUtils.getCurrentCommitHash(project)

        thread {
            try {
                executeAiderCommand(markdownDialog)
            } catch (e: Exception) {
                handleExecutionError(e, markdownDialog)
            } finally {
                performPostExecutionTasks(markdownDialog, currentCommitHash)
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

    private fun executeAiderCommand(markdownDialog: MarkdownDialog) {
        CommandExecutor(project, commandData, markdownDialog).executeCommand()
    }

    private fun handleExecutionError(e: Exception, markdownDialog: MarkdownDialog) {
        LOG.error("Error executing Aider command", e)
        updateDialogProgress(markdownDialog, "Error executing Aider command: ${e.message}", "Aider Command Error")
        markdownDialog.startAutoCloseTimer()
    }

    private fun performPostExecutionTasks(markdownDialog: MarkdownDialog, currentCommitHash: String?) {
        markdownDialog.startAutoCloseTimer()
        refreshFiles(markdownDialog)
        openGitComparisonToolIfNeeded(markdownDialog, currentCommitHash)
        markdownDialog.focus()
    }

    private fun refreshFiles(markdownDialog: MarkdownDialog) {
        val files = commandData.files.mapNotNull {
            VirtualFileManager.getInstance().findFileByUrl(it.filePath)
        }.toTypedArray()
        FileRefresher.refreshFiles(files, markdownDialog)
    }

    private fun openGitComparisonToolIfNeeded(markdownDialog: MarkdownDialog, currentCommitHash: String?) {
        val settings = AiderSettings.getInstance(project)
        if (settings.showGitComparisonTool) {
            currentCommitHash?.let { GitUtils.openGitComparisonTool(project, it) { markdownDialog.focus(1000) } }
        }
    }

    private fun updateDialogProgress(markdownDialog: MarkdownDialog, message: String, title: String) {
        invokeLater {
            markdownDialog.updateProgress(message, title)
        }
    }

}
