package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileTraversal
import java.io.File

class DocumentEachFolderAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        documentEachFolder(project, files)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun documentEachFolder(project: Project, virtualFiles: Array<VirtualFile>) {
            val settings = getInstance()

            val documentationFiles = virtualFiles.filter { it.isDirectory }.map { folder ->
                val allFiles = FileTraversal.traverseFilesOrDirectories(arrayOf(folder))
                val fileNames = allFiles.map { File(it.filePath).name }
                val fileDataList = allFiles.map { FileData(it.filePath, it.isReadOnly) }
                val filename = "${folder.name}.md"

                val commandData = CommandData(
                    message = """Generate a markdown documentation for the code in the provided files and directories: $fileNames. 
                        |Store the results in $folder/$filename.
                        |If there are exceptional implementation details, mention them.
                        |
                        |Good code documentation should provide a high-level overview of the module's purpose and functionality.
                        |It should clearly describe the module's role within the larger system and how it interacts with other modules.
                        |Include details on the module's public interfaces, key classes, and methods, as well as any design patterns used.
                        |Document the dependencies and data flow between this module and others, highlighting any critical integration points.
                        |This helps maintainers and developers understand the module's context and its impact on the overall system architecture.
                        |If the file already exists, update it instead.
                        |""".trimMargin(),
                    useYesFlag = true,
                    llm = settings.llm,
                    additionalArgs = settings.additionalArgs,
                    files = fileDataList,
                    isShellMode = false,
                    lintCmd = settings.lintCmd,
                    deactivateRepoMap = settings.deactivateRepoMap,
                    editFormat = settings.editFormat,
                    projectPath = project.basePath ?: "",
                    structuredMode = false
                )
                IDEBasedExecutor(project, commandData).execute()
                FileData(File(folder.path, filename).path, true)
            }
            // wait for all threads to finish
            if (documentationFiles.isNotEmpty()) {
                val summaryCommandData = CommandData(
                    message = """Summarize the following documentation files: ${documentationFiles.joinToString(", ") { it.filePath }}.
                        |Provide a concise overview of the key points and any notable details.
                        |""".trimMargin(),
                    useYesFlag = true,
                    llm = settings.llm,
                    additionalArgs = settings.additionalArgs,
                    files = documentationFiles,
                    isShellMode = false,
                    lintCmd = settings.lintCmd,
                    deactivateRepoMap = settings.deactivateRepoMap,
                    editFormat = settings.editFormat,
                    projectPath = project.basePath ?: "",
                    structuredMode = false
                )
                IDEBasedExecutor(project, summaryCommandData).execute()
            }
        }
    }
}
