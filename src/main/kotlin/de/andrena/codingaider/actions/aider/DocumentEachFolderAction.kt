package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileTraversal
import de.andrena.codingaider.utils.GitUtils
import java.io.File
import java.util.concurrent.CountDownLatch
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

    data class DocumentationAction(
        val isFinished: CountDownLatch,
        val documentationFileData: FileData
    )

    companion object {
        fun documentEachFolder(project: Project, virtualFiles: Array<VirtualFile>) {
            val settings = getInstance()
            val currentCommitHash = GitUtils.getCurrentCommitHash(project)
            val documentationLlm =
                if (settings.documentationLlm == "Default") settings.llm else settings.documentationLlm
            
            // Get the first enabled document type or use the Technical Documentation type
            val documentType = AiderProjectSettings.getInstance(project).getDocumentTypes()
                .firstOrNull { it.isEnabled } 
                ?: de.andrena.codingaider.features.documentation.DefaultDocumentTypes.getDefaultDocumentTypes().first()
            
            val documentationActions = virtualFiles.filter { it.isDirectory }.map { folder ->
                val allFiles = project.service<FileDataCollectionService>().collectAllFiles(arrayOf(folder))
                val fileNames = allFiles.map { File(it.filePath).name }
                val fileDataList = allFiles.map { FileData(it.filePath, it.isReadOnly) }
                val filename = "${folder.name}.md"

                // Create markdown file
                val markdownFile = File(folder.path, filename)
                // Ensure file exists and is writable
                markdownFile.createNewFile()

                val writableFileDataList = fileDataList + listOf(
                    FileData(markdownFile.path, false)
                )
                
                // Get context files for the document type
                val absoluteDocumentType = documentType.withAbsolutePaths(project.basePath ?: "")
                val contextFiles = absoluteDocumentType.contextFiles.map { FileData(it, false) }
                
                // Build the prompt using the DocumentationGenerationPromptService
                val promptService = project.service<de.andrena.codingaider.features.documentation.DocumentationGenerationPromptService>()
                val prompt = promptService.buildPrompt(
                    documentType,
                    allFiles,
                    markdownFile.path,
                    getAdditionalPromptForFolder(folder)
                )

                val commandData = CommandData(
                    message = prompt,
                    useYesFlag = true,
                    llm = documentationLlm,
                    additionalArgs = settings.additionalArgs,
                    files = writableFileDataList + contextFiles,
                    lintCmd = settings.lintCmd,
                    deactivateRepoMap = settings.deactivateRepoMap,
                    editFormat = settings.editFormat,
                    projectPath = project.basePath ?: "",
                    options = CommandOptions(disablePresentation = true, autoCloseDelay = 1),
                )
                val ideBasedExecutor = IDEBasedExecutor(project, commandData)
                ideBasedExecutor.execute()
                DocumentationAction(
                    ideBasedExecutor.isFinished(),
                    FileData(File(folder.path, filename).path, true)
                )
            }
            thread {
                documentationActions.forEach { it.isFinished.await() }
                if (documentationActions.isNotEmpty()) {
                    val documentationFiles = documentationActions.map { it.documentationFileData }
                    // Create overview markdown file
                    val overviewMarkdownFile = File(project.basePath, "overview.md")

                    // Ensure file exists and is writable
                    overviewMarkdownFile.createNewFile()

                    val writableSummaryFiles = documentationFiles + listOf(
                        FileData(overviewMarkdownFile.path, false)
                    )

                    val summaryCommandData = CommandData(
                        message = getOverviewPrompt(documentationFiles),
                        useYesFlag = true,
                        llm = documentationLlm,
                        additionalArgs = settings.additionalArgs,
                        files = writableSummaryFiles,
                        lintCmd = settings.lintCmd,
                        deactivateRepoMap = settings.deactivateRepoMap,
                        editFormat = settings.editFormat,
                        projectPath = project.basePath ?: "",
                        options = CommandOptions(
                            disablePresentation = false,
                            commitHashToCompareWith = currentCommitHash,
                        )
                    )
                    IDEBasedExecutor(
                        project,
                        summaryCommandData,
                    ).execute()
                }
            }
        }

        private fun getOverviewPrompt(documentationFiles: List<FileData>) =
            """Summarize the following documentation files: ${documentationFiles.joinToString(", ") { it.filePath }}.
|Provide a concise overview of the key points and any notable details.
|Include relevant links to the documentation files.
|IMPORTANT: The dependencies between the individual modules in the project should be described using a Mermaid diagram embedded in the markdown file.
|Focus on a high level overview of the project and its modules.
|Make sure files are linked using relative paths.
|Store the results in the root folder of the project in the file "overview.md".
|To calculate the dependencies, use the existing mermaid diagrams in the documentation files.
|If an overview documentation file is already available, update it instead of creating a new one.
|""".trimMargin()

        private fun getAdditionalPromptForFolder(folder: VirtualFile): String {
            return """
|Document the dependencies between classes in the module ${folder.name} and classes of the project outside of the ${folder.name} module using a Mermaid diagram embedded in the markdown file.
|Make sure files are linked using relative paths.
|""".trimMargin()
        }
    }
}
