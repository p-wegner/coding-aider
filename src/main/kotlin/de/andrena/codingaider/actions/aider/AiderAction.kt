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
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance


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
                val aiderIgnoreService = project.service<AiderIgnoreService>()
                val filteredFiles = files.filterNot { aiderIgnoreService.isIgnored(it.path) }.toTypedArray()
                
                if (filteredFiles.isEmpty()) {
                    com.intellij.openapi.ui.Messages.showInfoMessage(
                        project,
                        "All selected files are ignored by .aiderignore",
                        "No Files to Process"
                    )
                    return
                }
                
                val allFiles = project.service<FileDataCollectionService>().collectAllFiles(filteredFiles)

                if (directShellMode) {
                    val commandData = collectDefaultShellCommandData(allFiles, project)
                    ShellExecutor(project, commandData).execute()
                } else {
                    val dialog = AiderInputDialog(project, allFiles)
                    if (dialog.showAndGet()) {
                        val commandData = collectCommandData(dialog, project)
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
            val settings = getInstance()
            return CommandData(
                message = dialog.getInputText(),
                useYesFlag = dialog.isYesFlagChecked(),
                llm = dialog.getLlm().name,
                additionalArgs = dialog.getAdditionalArgs(),
                files = dialog.getAllFiles(),
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                aiderMode = dialog.selectedMode,
                sidecarMode = settings.useSidecarMode
            )
        }

        fun collectCommandData(files: List<FileData>, message: String, project: Project, mode: AiderMode): CommandData {
            val settings = getInstance()
            return CommandData(
                message = message,
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = files,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                aiderMode = mode,
                sidecarMode = settings.useSidecarMode
            )
        }

        private fun collectDefaultShellCommandData(files: List<FileData>, project: Project): CommandData {
            val settings = getInstance()
            return CommandData(
                message = "",
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = files,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                aiderMode = AiderMode.SHELL
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

