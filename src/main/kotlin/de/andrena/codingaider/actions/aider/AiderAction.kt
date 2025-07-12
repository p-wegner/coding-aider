package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.executors.api.ShellExecutor
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderDialogStateService
import de.andrena.codingaider.services.AiderIgnoreService
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.CommandDataCollector


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
                val allFiles = project.service<FileDataCollectionService>().collectAllFiles(files)
                
                if (allFiles.isEmpty()) {
                    com.intellij.openapi.ui.Messages.showInfoMessage(
                        project,
                        "All selected files are ignored by .aiderignore",
                        "No Files to Process"
                    )
                    return
                }

                if (directShellMode) {
                    val commandData = CommandDataCollector.collectForShellMode(allFiles, project)
                    ShellExecutor(project, commandData).execute()
                } else {
                    val dialog = AiderInputDialog(project, allFiles)
                    if (dialog.showAndGet()) {
                        val commandData = CommandDataCollector.collectFromDialog(dialog, project)
                        AiderDialogStateService.getInstance(project).saveState(
                            dialog.getInputText(),
                            dialog.isYesFlagChecked(),
                            dialog.getLlm().name,
                            dialog.getAdditionalArgs(),
                            dialog.getAllFiles(),
                            dialog.isShellMode(),
                            dialog.isStructuredMode(),
                            dialog.selectedMode
                        )
                        executeAiderActionWithCommandData(project, commandData)
                    }
                }
            }
        }


        fun executeAiderActionWithCommandData(project: Project, commandData: CommandData) {
            if (commandData.isShellMode) {
                ShellExecutor(project, commandData).execute()
            } else {
                IDEBasedExecutor(project, commandData).execute()
            }
        }

        fun collectCommandData(dialog: AiderInputDialog, project: Project): CommandData {
            return CommandDataCollector.collectFromDialog(dialog, project)
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

