package de.andrena.codingaider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.executors.ShellExecutor
import de.andrena.codingaider.history.AiderHistoryHandler
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.inputdialog.PersistentFileManager
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.settings.AiderDefaults
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileTraversal


class AiderAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        executeAiderAction(e, false)
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun executeAiderAction(e: AnActionEvent, directShellMode: Boolean) {
            val project: Project? = e.project
            val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

            if (project != null && !files.isNullOrEmpty()) {
                val persistentFileManager = PersistentFileManager(project.basePath ?: "")
                val allFiles = FileTraversal.traverseFilesOrDirectories(files).toMutableList()

                allFiles.addAll(persistentFileManager.getPersistentFiles())

                if (directShellMode) {
                    val commandData = collectDefaultCommandData(allFiles, project)
                    ShellExecutor(project, commandData).execute()
                } else {
                    val dialog = AiderInputDialog(
                        project,
                        allFiles.distinctBy { it.filePath }
                    )
                    if (dialog.showAndGet()) {
                        val commandData = collectCommandData(dialog)
                        if (commandData.isShellMode) {
                            ShellExecutor(project, commandData).execute()
                        } else {
                            IDEBasedExecutor(project, commandData).execute()
                        }
                    }
                }
            }
        }

        private fun collectCommandData(dialog: AiderInputDialog): CommandData {
            val settings = AiderSettings.getInstance(dialog.project)
            return CommandData(
                message = dialog.getInputText(),
                useYesFlag = dialog.isYesFlagChecked(),
                llm = dialog.getLlm(),
                additionalArgs = dialog.getAdditionalArgs(),
                files = dialog.getAllFiles(),
                isShellMode = dialog.isShellMode(),
                lintCmd = settings.lintCmd
            )
        }

        private fun collectDefaultCommandData(files: List<FileData>, project: Project): CommandData {
            val settings = AiderSettings.getInstance(project)
            return CommandData(
                message = "",
                useYesFlag = AiderDefaults.USE_YES_FLAG,
                llm = AiderDefaults.LLM,
                additionalArgs = AiderDefaults.ADDITIONAL_ARGS,
                files = files,
                isShellMode = true, // Always true for direct shell mode
                lintCmd = settings.lintCmd
            )
        }
    }
}

class AiderShellAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        AiderAction.executeAiderAction(e, true)
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

class ShowLastCommandResultAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val historyHandler = AiderHistoryHandler(project.basePath ?: "")
        val lastCommandResult = historyHandler.getLastChatHistory()

        val dialog = MarkdownDialog(project, "Last Aider Command Result", lastCommandResult)
        dialog.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

