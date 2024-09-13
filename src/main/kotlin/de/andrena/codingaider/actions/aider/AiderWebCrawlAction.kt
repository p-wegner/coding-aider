package de.andrena.codingaider.actions.aider

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.PersistentFileManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileRefresher
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.security.MessageDigest

class AiderWebCrawlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = AiderSettings.getInstance(project)
        val url =
            Messages.showInputDialog(project, "Enter URL to crawl:", "Aider Web Crawl", Messages.getQuestionIcon())
        if (!url.isNullOrEmpty()) {
            val projectRoot = project.basePath ?: "."
            val domain = URI(url).host
            val docsPath = "$projectRoot/.aider-docs/$domain"
            File(docsPath).mkdirs()

            val combinedHash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).let {
                BigInteger(1, it).toString(16).padStart(32, '0')
            }

            val pageName = URL(url).path.split("/").lastOrNull() ?: "index"
            val fileName = "$pageName-$combinedHash.md"
            val filePath = "$docsPath/$fileName"
            val file = File(filePath)

            if (!file.exists()) {
                crawlAndProcessWebPage(url, file)

                val commandData = CommandData(
                    message = """
                        Clean up and simplify the provided file $fileName using whole file edit format. Follow these guidelines:
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
                    """.trimIndent(),
                    useYesFlag = true,
                    llm = settings.webCrawlLlm,
                    additionalArgs = "",
                    files = listOf(FileData(filePath, false)),
                    isShellMode = false,
                    lintCmd = "",
                    projectPath = project.basePath ?: ""
                )
                val settings = AiderSettings.getInstance(project)
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

    private fun crawlAndProcessWebPage(url: String, file: File) {
        val webClient = WebClient()
        webClient.options.isJavaScriptEnabled = false
        val page: HtmlPage = webClient.getPage(url)
        val htmlContent = page.asXml()
        val markdown = FlexmarkHtmlConverter.builder().build().convert(htmlContent)
        file.writeText(markdown)
    }

    private fun refreshAndAddFile(project: com.intellij.openapi.project.Project, filePath: String) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(filePath))
        val persistentFileManager = PersistentFileManager(project.basePath ?: "")
        persistentFileManager.addFile(FileData(filePath, true))
        if (virtualFile != null) {
            FileRefresher.refreshFiles(arrayOf(virtualFile))
        }
    }

    private fun showNotification(
        project: com.intellij.openapi.project.Project,
        content: String,
        type: NotificationType
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Aider Web Crawl")
            .createNotification(content, type)
            .notify(project)
    }
}
