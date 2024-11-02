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
import kotlin.concurrent.thread

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

            val documentationActions = virtualFiles.filter { it.isDirectory }.map { folder ->
                val allFiles = FileTraversal.traverseFilesOrDirectories(arrayOf(folder))
                val fileNames = allFiles.map { File(it.filePath).name }
                val fileDataList = allFiles.map { FileData(it.filePath, it.isReadOnly) }
                val filename = "${folder.name}.md"

                // Create markdown and PlantUML files
                val markdownFile = File(folder.path, filename)
                val pumlFilename = "${folder.name}.puml"
                val pumlFile = File(folder.path, pumlFilename)

                // Ensure files exist and are writable
                markdownFile.createNewFile()
                pumlFile.createNewFile()

                val writableFileDataList = fileDataList + listOf(
                    FileData(markdownFile.path, false),
                    FileData(pumlFile.path, false)
                )

                val commandData = CommandData(
                    message = """Generate a markdown documentation for the code in the provided files and directories: $fileNames. 
                            |Store the results in $folder/$filename.
                            |If there are exceptional implementation details, mention them.
                            |
                            |Good code documentation should provide a high-level overview of the module's purpose and functionality.
                            |Important files should be clearly described and linked in the documentation file.
                            |Include details on the module's public interfaces, key classes, and methods, as well as any design patterns used.
                            |Document the dependencies and data flow between this module and others of the project. 
                            |This should be done using PlantUML and stored as $folder/$pumlFilename.
                            |Make sure files are linked using relative paths.
                            |If the file already exists, update it instead.
                            |""".trimMargin(),
                    useYesFlag = true,
                    llm = settings.webCrawlLlm,
                    additionalArgs = settings.additionalArgs,
                    files = writableFileDataList,
                    isShellMode = false,
                    lintCmd = settings.lintCmd,
                    deactivateRepoMap = settings.deactivateRepoMap,
                    editFormat = settings.editFormat,
                    projectPath = project.basePath ?: "",
                    structuredMode = false
                )
                val ideBasedExecutor = IDEBasedExecutor(project, commandData)
                ideBasedExecutor.execute()
                ideBasedExecutor.isFinished() to FileData(File(folder.path, filename).path, true)
            }

            thread {
                documentationActions.forEach { it.first.await() }
                if (documentationActions.isNotEmpty()) {
                    val documentationFiles = documentationActions.map { it.second }
                    // Create overview markdown and PlantUML files
                    val overviewMarkdownFile = File(project.basePath, "overview.md")
                    val overviewPumlFile = File(project.basePath, "overview.puml")

                    // Ensure files exist and are writable
                    overviewMarkdownFile.createNewFile()
                    overviewPumlFile.createNewFile()

                    val writableSummaryFiles = documentationFiles + listOf(
                        FileData(overviewMarkdownFile.path, false),
                        FileData(overviewPumlFile.path, false)
                    )

                    val summaryCommandData = CommandData(
                        message = """Summarize the following documentation files: ${documentationFiles.joinToString(", ") { it.filePath }}.
                        |Provide a concise overview of the key points and any notable details.
                        |Include relevant links to the documentation files.
                        |IMPORTANT: The dependencies between the individual modules in the project should be described using PlantUML and stored as a separate file.
                        |Make sure files are linked using relative paths.
                        |Store the results in the root folder of the project and name it "overview.md".
                        |Store the results in the root folder of the project and name it "overview.md".
                        |The dependencies should be described using PlantUML and stored as "overview.puml".
                        |""".trimMargin(),
                        useYesFlag = true,
                        llm = settings.webCrawlLlm,
                        additionalArgs = settings.additionalArgs,
                        files = writableSummaryFiles,
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
}
