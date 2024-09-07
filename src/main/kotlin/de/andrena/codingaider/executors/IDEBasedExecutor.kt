package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.executors.CommandExecutor
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import java.awt.EventQueue.invokeLater
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
        val commandExecutor = CommandExecutor(project, commandData, markdownDialog)
        commandExecutor.executeCommand()
    }

    private fun handleExecutionError(e: Exception, markdownDialog: MarkdownDialog) {
        LOG.error("Error executing Aider command", e)
        updateDialogProgress(markdownDialog, "Error executing Aider command: ${e.message}", "Aider Command Error")
        markdownDialog.startAutoCloseTimer()
    }

    private fun performPostExecutionTasks(markdownDialog: MarkdownDialog, currentCommitHash: String?) {
        refreshFiles(markdownDialog)
        openGitComparisonToolIfNeeded(currentCommitHash)
    }

    private fun refreshFiles(markdownDialog: MarkdownDialog) {
        val files = commandData.files.mapNotNull {
            VirtualFileManager.getInstance().findFileByUrl(it.filePath)
        }.toTypedArray()
        FileRefresher.refreshFiles(files, markdownDialog)
    }

    private fun openGitComparisonToolIfNeeded(currentCommitHash: String?) {
        val settings = AiderSettings.getInstance(project)
        if (settings.showGitComparisonTool) {
            invokeLater {
                currentCommitHash?.let { GitUtils.openGitComparisonTool(project, it) }
            }
        }
    }

    private fun updateDialogProgress(markdownDialog: MarkdownDialog, message: String, title: String) {
        invokeLater {
            markdownDialog.updateProgress(message, title)
        }
    }

    private fun pollProcessAndReadOutput(
        process: Process,
        output: StringBuilder,
        markdownDialog: MarkdownDialog
    ) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val startTime = System.currentTimeMillis()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
            LOG.info("Aider output: $line")
            val runningTime = (System.currentTimeMillis() - startTime) / 1000
            updateDialogProgress(markdownDialog, output.toString(), "Aider command in progress ($runningTime seconds)")
            if (!process.isAlive || runningTime > 300) { // 5 minutes timeout
                break
            }
            Thread.sleep(10) // Small delay to prevent UI freezing
        }
    }
}
