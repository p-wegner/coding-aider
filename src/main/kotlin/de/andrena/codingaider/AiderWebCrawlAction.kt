package de.andrena.codingaider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.intellij.openapi.ui.Messages

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
            Messages.showInfoMessage(project, markdown, "Converted Markdown")
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
