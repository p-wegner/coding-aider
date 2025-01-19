package de.andrena.codingaider.outputview

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.data.MutableDataSet

class MarkdownJcefViewer {

    private val mainPanel: JPanel = JPanel(BorderLayout())
    private var jbCefBrowser: JBCefBrowser? = null

    init {
        if (JBCefApp.isSupported()) {
            // Create the JCEF Browser
            jbCefBrowser = JBCefBrowser()
            // Add the browser component to our mainPanel
            mainPanel.add(jbCefBrowser!!.component, BorderLayout.CENTER)
        } else {
            // Fallback if JCEF is not supported
            mainPanel.add(
                javax.swing.JLabel("JCEF is not supported on this IDE/platform."),
                BorderLayout.CENTER
            )
        }
    }

    val component: JComponent
        get() = mainPanel

    fun setMarkdown(markdown: String) {
        jbCefBrowser?.let { browser ->
            val html = convertMarkdownToHtml(markdown)
            val encodedHtml = Base64.getEncoder().encodeToString(html.toByteArray(Charsets.UTF_8))
            browser.loadURL("data:text/html;base64,$encodedHtml")
        }
    }
    private val options = MutableDataSet().apply {
        set(
            Parser.EXTENSIONS, listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create(),
                TaskListExtension.create(),
                DefinitionExtension.create(),
                FootnoteExtension.create(),
                TocExtension.create()
            )
        )
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        val parser = Parser.builder(options).build()
        val document = parser.parse(markdown)
        val renderer = HtmlRenderer.builder(options).build()
        return renderer.render(document)
    }

}
