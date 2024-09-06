package de.andrena.codingaider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.inputdialog.PersistentFileManager
import de.andrena.codingaider.utils.FileRefresher
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.net.URL

class AiderWebCrawlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url =
            Messages.showInputDialog(project, "Enter URL to crawl:", "Aider Web Crawl", Messages.getQuestionIcon())
        if (!url.isNullOrEmpty()) {
            val webClient = WebClient()
            webClient.options.isJavaScriptEnabled = false
            val page: HtmlPage = webClient.getPage(url)
            val htmlContent = page.asXml()
            val markdown = FlexmarkHtmlConverter.builder().build().convert(htmlContent)
            val urlHash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).let {
                BigInteger(1, it).toString(16).padStart(32, '0')
                // Add the markdown file to persistent files
                val persistentFileManager = PersistentFileManager(project.basePath ?: "")
                persistentFileManager.addFile(FileData("$projectRoot/.aider-docs/$fileName", false))
            }
            val pageName = URL(url).path.split("/").lastOrNull() ?: "index"
            val fileName = "$pageName-$urlHash.md"
            val projectRoot = project.basePath ?: "."
            File("$projectRoot/.aider-docs").mkdirs()
            File("$projectRoot/.aider-docs/$fileName").writeText(markdown)
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File("$projectRoot/.aider-docs/$fileName"))
            if (virtualFile != null) {
                FileRefresher.refreshFiles(project, arrayOf(virtualFile))
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
