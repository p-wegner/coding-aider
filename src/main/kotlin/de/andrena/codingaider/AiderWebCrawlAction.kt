package de.andrena.codingaider

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.inputdialog.PersistentFileManager
import de.andrena.codingaider.utils.FileRefresher
import java.io.File
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest

class AiderWebCrawlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url =
            Messages.showInputDialog(project, "Enter URL to crawl:", "Aider Web Crawl", Messages.getQuestionIcon())
        if (!url.isNullOrEmpty()) {
            val projectRoot = project.basePath ?: "."
            File("$projectRoot/.aider-docs").mkdirs()

            val combinedHash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).let {
                BigInteger(1, it).toString(16).padStart(32, '0')
            }

            val pageName = URL(url).path.split("/").lastOrNull() ?: "index"
            val fileName = "$pageName-$combinedHash.md"
            val filePath = "$projectRoot/.aider-docs/$fileName"

            if (!File(filePath).exists()) {
                val webClient = WebClient()
                webClient.options.isJavaScriptEnabled = true
                val page: HtmlPage = webClient.getPage(url)
                val htmlContent = page.asXml()
                val markdown = FlexmarkHtmlConverter.builder().build().convert(htmlContent)
                File(filePath).writeText(markdown)
            }

            val virtualFile =
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(filePath))
            val persistentFileManager = PersistentFileManager(project.basePath ?: "")
            persistentFileManager.addFile(FileData(filePath, true))
            if (virtualFile != null) {
                FileRefresher.refreshFiles(project, arrayOf(virtualFile))
            }

            Messages.showInfoMessage(project, "File saved: $filePath", "Aider Web Crawl")
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}