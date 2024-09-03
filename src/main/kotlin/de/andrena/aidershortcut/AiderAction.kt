package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.aidershortcut.command.CommandData
import de.andrena.aidershortcut.executors.IDEBasedExecutor
import de.andrena.aidershortcut.executors.ShellExecutor
import de.andrena.aidershortcut.inputdialog.AiderInputDialog

class AiderAction : AnAction() {
    private val LOG = Logger.getInstance(AiderAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val contextHandler = AiderContextHandler(project.basePath ?: "")
            val persistentFiles = contextHandler.loadPersistentFiles()
            val allFiles = (files.map { it.path } + persistentFiles).distinct()

            val dialog = AiderInputDialog(project, allFiles)
            if (dialog.showAndGet()) {
                val commandData = collectCommandData(dialog, allFiles)
                if (commandData.isShellMode) {
                    ShellExecutor(project, commandData).execute()
                } else {
                    IDEBasedExecutor(project, commandData, files).execute()
                }
                dialog.addToHistory(commandData.message)
            }
        }
    }

    private fun collectCommandData(dialog: AiderInputDialog, allFiles: List<String>): CommandData {
        return CommandData(
            message = dialog.getInputText(),
            useYesFlag = dialog.isYesFlagChecked(),
            selectedCommand = dialog.getSelectedCommand(),
            additionalArgs = dialog.getAdditionalArgs(),
            filePaths = allFiles, // Changed to pass the list directly
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
