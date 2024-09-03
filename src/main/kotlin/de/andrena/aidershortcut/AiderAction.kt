package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
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
            val allFiles = files.map { it.path } + persistentFiles

            val dialog = AiderInputDialog(project, allFiles)
            if (dialog.showAndGet()) {
                val commandData = collectCommandData(dialog, allFiles)
                if (commandData.isShellMode) {
                    ShellExecutor(project, commandData).execute()
                } else {
                    IDEBasedExecutor(
                        project,
                        commandData,
                        dialog.getWriteableFiles().map { VirtualFileManager.getInstance().findFileByUrl(it) }
                            .toTypedArray()
                    ).execute() // Pass writeableFiles as VirtualFile array
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
            writeableFiles = dialog.getWriteableFiles(), // Get writeable files from dialog
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
