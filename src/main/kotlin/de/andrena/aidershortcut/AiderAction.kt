package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.EventQueue.invokeLater
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class AiderAction : AnAction() {
    private val LOG = Logger.getInstance(AiderAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val dialog = AiderInputDialog(project, files.map { it.path })
            if (dialog.showAndGet()) {
                val commandData = collectCommandData(dialog, files)
                if (commandData.isShellMode) {
                    ShellExecutor(project, commandData).execute()
                } else {
                    TerminalExecutor(project, commandData, files).execute()
                }
                // Add the command to history
                dialog.addToHistory(commandData.message)
            }
        }
    }

    private fun collectCommandData(dialog: AiderInputDialog, files: Array<VirtualFile>): CommandData {
        return CommandData(
            message = dialog.getInputText(),
            useYesFlag = dialog.isYesFlagChecked(),
            selectedCommand = dialog.getSelectedCommand(),
            additionalArgs = dialog.getAdditionalArgs(),
            filePaths = files.joinToString(" ") { it.path },
            readOnlyFiles = dialog.getReadOnlyFiles(),
            isShellMode = dialog.isShellMode()
        )
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }
}
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun getHistory(): List<Pair<LocalDateTime, String>> {
        if (!historyFile.exists()) return emptyList()

        return historyFile.readLines()
            .chunked(2)
            .mapNotNull { chunk ->
                if (chunk.size == 2 && chunk[0].startsWith("# ")) {
                    val dateTime = LocalDateTime.parse(chunk[0].substring(2), dateTimeFormatter)
                    val command = chunk[1].trim()
                    dateTime to command
                } else null
            }
            .reversed()
    }

    fun addToHistory(command: String) {
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        historyFile.appendText("# $timestamp\n$command\n")
    }
}
