package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.aidershortcut.command.CommandData
import de.andrena.aidershortcut.command.FileData
import de.andrena.aidershortcut.executors.IDEBasedExecutor
import de.andrena.aidershortcut.executors.ShellExecutor
import de.andrena.aidershortcut.inputdialog.AiderInputDialog

class AiderAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val allFiles = mutableListOf<FileData>()

            files.forEach { file ->
                if (file.isDirectory) {
                    addFilesFromDirectory(file, allFiles)
                } else {
                    allFiles.add(FileData(file.path, false))
                }
            }

            val dialog = AiderInputDialog(project, allFiles)
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

    private fun addFilesFromDirectory(directory: VirtualFile, allFiles: MutableList<FileData>) {
        directory.children.forEach { childFile ->
            if (childFile.isDirectory) {
                addFilesFromDirectory(childFile, allFiles)
            } else {
                allFiles.add(FileData(childFile.path, false))
            }
        }
    }

    private fun collectCommandData(dialog: AiderInputDialog): CommandData {
        return CommandData(
            message = dialog.getInputText(),
            useYesFlag = dialog.isYesFlagChecked(),
            selectedCommand = dialog.getSelectedCommand(),
            additionalArgs = dialog.getAdditionalArgs(),
            files = dialog.getAllFiles(),
            isShellMode = dialog.isShellMode()
        )
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }
}
