package de.andrena.codingaider.actions.aider

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER
import de.andrena.codingaider.services.AiderEditFormat
import de.andrena.codingaider.services.MarkdownConversionService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest

class AiderWebCrawlAction : AnAction() {


    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = getInstance()
        val url =
            Messages.showInputDialog(project, "Enter URL to crawl:", "Aider Web Crawl", Messages.getQuestionIcon())
        if (!url.isNullOrEmpty()) {
            val projectRoot = project.basePath ?: "."
            val domain = URI(url).host
            val docsPath = "$projectRoot/$AIDER_DOCS_FOLDER/$domain"
            File(docsPath).mkdirs()

            val combinedHash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).let {
                BigInteger(1, it).toString(16).padStart(32, '0')
            }

            val pageName = URI(url).toURL().path.split("/").lastOrNull() ?: "index"
            val fileName = "$pageName-raw-$combinedHash.md"
            val filePath = "$docsPath/$fileName"
            val file = File(filePath)

            if (!file.exists()) {
                crawlAndProcessWebPage(url, file, project)

                val commandData = CommandData(
                    message = """
                        Clean up and simplify the provided file $fileName. Follow these guidelines:
                        1. Remove all navigation elements, headers, footers, and sidebars.
                        2. Delete any advertisements, banners, or promotional content.
                        3. Remove or simplify the table of contents, keeping only if it's essential for understanding the structure.
                        4. Strip out all internal page links, but keep external links that point to important resources.
                        5. Remove any repetitive elements or boilerplate text.
                        6. Simplify complex HTML structures, converting them to clean markdown where possible.
                        7. Keep all information relevant for code documentation and how to use the technology described.
                        8. Organize the content in a logical flow, using appropriate markdown headers.
                        9. If code snippets are present, ensure they are properly formatted in markdown code blocks.
                        10. Remove any content that seems out of context or irrelevant to the main topic.
                        11. Summarize lengthy paragraphs while retaining key information.
                        12. Ensure the final document is concise, well-structured, and focused on the core technical content.
                        Important: Make sure to save the simplified markdown documentation in a separate file without the raw infix and not in the same file as the initial content.
                    } 
                    """.trimIndent(),
                    useYesFlag = true,
                    llm = settings.webCrawlLlm,
                    additionalArgs = "",
                    files = listOf(FileData(filePath, false)),
                    lintCmd = "",
                    projectPath = project.basePath ?: "",
                    editFormat = AiderEditFormat.WHOLE.value,
                    aiderMode = AiderMode.NORMAL,
                    options = CommandOptions(autoCommit = false, dirtyCommits = false, summarizedOutput = false),
                )
                if (settings.activateIdeExecutorAfterWebcrawl) {
                    IDEBasedExecutor(project, commandData).execute()
                }
                // Refresh the file and add it to PersistentFileManager
                refreshAndAddFile(project, filePath)

                // Notify the user about the next steps
                showNotification(
                    project,
                    "Web page crawled and processed. The file has been added to persistent files.",
                    NotificationType.INFORMATION
                )
            } else {
                // Notify the user that the file already exists
                showNotification(project, "The file already exists. No action taken.", NotificationType.INFORMATION)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun crawlAndProcessWebPage(url: String, file: File, project: Project) {
        val webClient = WebClient()
        webClient.options.apply {
            isJavaScriptEnabled = false
            isThrowExceptionOnScriptError = false
            isThrowExceptionOnFailingStatusCode = false
            isCssEnabled = false
        }

        val page: HtmlPage = webClient.getPage(url)
        val htmlContent = page.asXml()

        val markdown = project.getService(MarkdownConversionService::class.java)
            .convertHtmlToMarkdown(htmlContent, url)

        file.writeText(markdown)
    }

    private fun refreshAndAddFile(project: Project, filePath: String) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(filePath))
        val persistentFileService = project.getService(PersistentFileService::class.java)
        persistentFileService.addFile(FileData(filePath, true))
        if (virtualFile != null) {
            FileRefresher.refreshFiles(arrayOf(virtualFile))
        }
    }

    private fun showNotification(
        project: Project,
        content: String,
        type: NotificationType
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Aider Web Crawl")
            .createNotification(content, type)
            .notify(project)
    }
}

