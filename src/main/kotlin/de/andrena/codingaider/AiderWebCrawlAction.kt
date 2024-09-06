package de.andrena.codingaider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.intellij.openapi.ui.Messages
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
            }
            val pageName = URL(url).path.split("/").lastOrNull() ?: "index"
            val fileName = "$pageName-$urlHash.md"
            File(fileName).writeText(markdown)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
