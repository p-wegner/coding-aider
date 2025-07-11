package de.andrena.codingaider.actions.aider

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.features.documentation.dialogs.GitRepoDocumentationDialog
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER
import de.andrena.codingaider.services.AiderEditFormat
import de.andrena.codingaider.services.MarkdownConversionService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import java.awt.Dimension
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import javax.swing.JComponent

class AiderWebCrawlAction : AnAction() {

    private class WebCrawlAndGitDialog(private val project: Project) : DialogWrapper(project) {
        private val urlField = JBTextField().apply {
            emptyText.text = "Enter URL to crawl"
        }

        init {
            title = "Web Crawl & Git Repository Documentation"
            init()
        }

        override fun createCenterPanel(): JComponent {
            val tabbedPane = JBTabbedPane()
            
            // Web Crawl Tab
            val webCrawlPanel = panel {
                row {
                    label("Enter URL to crawl:")
                }
                row {
                    cell(urlField)
                        .resizableColumn()
                        .align(AlignX.FILL)
                }
                row {
                    text("This will crawl the web page and process it for documentation.")
                }
            }
            
            tabbedPane.addTab("Web Crawl", webCrawlPanel)
            
            // Git Repository Tab - placeholder for now, will be replaced with actual implementation
            val gitPanel = panel {
                row {
                    text("Git repository documentation functionality will be available here.")
                }
            }
            
            tabbedPane.addTab("Git Repository", gitPanel)
            
            val mainPanel = panel {
                row {
                    cell(tabbedPane)
                        .resizableColumn()
                        .align(AlignX.FILL)
                }.resizableRow()
            }
            
            mainPanel.preferredSize = Dimension(600, 400)
            return mainPanel
        }

        override fun doOKAction() {
            val selectedTab = (centerPanel as? JComponent)?.let { panel ->
                val tabbedPane = findTabbedPane(panel)
                tabbedPane?.selectedIndex
            } ?: 0

            when (selectedTab) {
                0 -> { // Web Crawl
                    val url = urlField.text.trim()
                    if (url.isNotEmpty()) {
                        super.doOKAction()
                        performWebCrawl(project, url)
                    } else {
                        Messages.showErrorDialog("Please enter a URL to crawl", "Error")
                    }
                }
                1 -> { // Git Repository
                    super.doOKAction()
                    val gitDialog = GitRepoDocumentationDialog(project)
                    gitDialog.show()
                }
            }
        }

        private fun findTabbedPane(component: JComponent): JBTabbedPane? {
            if (component is JBTabbedPane) return component
            for (child in component.components) {
                if (child is JComponent) {
                    val found = findTabbedPane(child)
                    if (found != null) return found
                }
            }
            return null
        }
    }


    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = WebCrawlAndGitDialog(project)
        dialog.show()
    }

    private fun performWebCrawl(project: Project, url: String) {
        val settings = getInstance()
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

            val processedFileName = "$pageName-$combinedHash.md"
            val processedFilePath = "$docsPath/$processedFileName"

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
                    Important: Make sure to save the simplified markdown documentation in a separate file named $processedFileName and not in the same file as the initial content.
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
                options = CommandOptions(autoCommit = false, dirtyCommits = false, promptAugmentation = false),
            )
            
            if (settings.activateIdeExecutorAfterWebcrawl) {
                val executor = IDEBasedExecutor(project, commandData) { success ->
                    if (success && File(processedFilePath).exists()) {
                        // Only add the processed markdown file to persistent files
                        refreshAndAddFile(project, processedFilePath)
                        showNotification(
                            project,
                            "Web page crawled and processed. The processed file has been added to persistent files.",
                            NotificationType.INFORMATION
                        )
                    } else {
                        showNotification(
                            project,
                            "Web page crawled but processing failed or file not found.",
                            NotificationType.WARNING
                        )
                    }
                }
                executor.execute()
            } else {
                // If not using IDE executor, just add the raw file
                refreshAndAddFile(project, filePath)
                showNotification(
                    project,
                    "Web page crawled. Raw file has been added to persistent files.",
                    NotificationType.INFORMATION
                )
            }
        } else {
            // Notify the user that the file already exists
            showNotification(project, "The file already exists. No action taken.", NotificationType.INFORMATION)
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

