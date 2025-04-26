package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import java.io.File

class DocumentCodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        documentCode(project, files)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun documentCode(project: Project, virtualFiles: Array<VirtualFile>) {
            val filename = Messages.showInputDialog(
                project,
                "Enter the filename to store the documentation:",
                "Document Code",
                Messages.getQuestionIcon()
            ) ?: return

            val allFiles = project.service<FileDataCollectionService>().collectAllFiles(virtualFiles)
            val fileNames = allFiles.map { File(it.filePath).name }

            val settings = getInstance()
            val commandData = CommandData(
                message = """Generate a markdown documentation for the code in the provided files and directories: $fileNames. 
                    |If there are exceptional implementation details, mention them. Store the results in $filename. 
                    |If the file already exists, update it instead.
                    |""".trimMargin(),
                useYesFlag = true,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = allFiles,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                sidecarMode = settings.useSidecarMode
            )
            IDEBasedExecutor(project, commandData).execute()
        }
    }
}
