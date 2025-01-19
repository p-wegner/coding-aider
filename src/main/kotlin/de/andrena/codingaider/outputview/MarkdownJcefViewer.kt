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
            jbCefBrowser = JBCefBrowser().apply {
                component.isFocusable = true
            }
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
        val html = renderer.render(document)
        
        val isDark = !com.intellij.ui.JBColor.isBright()
        val colors = if (isDark) {
            mapOf(
                "bodyBg" to "#2b2b2b",
                "bodyText" to "#ffffff",
                "preBg" to "#1e1e1e",
                "preBorder" to "#666666",
                "searchBg" to "#362a1e",
                "replaceBg" to "#1e3626",
                "searchText" to "#ff8c7c",
                "replaceText" to "#7cff8c"
            )
        } else {
            mapOf(
                "bodyBg" to "#ffffff", 
                "bodyText" to "#000000",
                "preBg" to "#f5f5f5",
                "preBorder" to "#cccccc",
                "searchBg" to "#ffedeb",
                "replaceBg" to "#ebffed", 
                "searchText" to "#d73a49",
                "replaceText" to "#28a745"
            )
        }

        return """
            <html>
            <head>
                <style>
                    body { 
                        font-family: sans-serif;
                        margin: 20px;
                        line-height: 1.6;
                        background: ${colors["bodyBg"]};
                        color: ${colors["bodyText"]};
                    }
                    pre {
                        background: ${colors["preBg"]};
                        border: 1px solid ${colors["preBorder"]};
                        padding: 10px;
                        margin: 0;
                    }
                    .search-block {
                        background: ${colors["searchBg"]};
                        color: ${colors["searchText"]};
                        padding: 4px 8px;
                    }
                    .replace-block {
                        background: ${colors["replaceBg"]};
                        color: ${colors["replaceText"]};
                        padding: 4px 8px;
                    }
                    .aider-intention {
                        background: ${if (isDark) "#1a2733" else "#f0f7ff"};
                        border: 1px solid ${if (isDark) "#2c4356" else "#bcd6f5"};
                        border-radius: 4px;
                        padding: 12px;
                        margin: 12px 0;
                        color: ${if (isDark) "#589df6" else "#0066cc"};
                    }
                    .aider-summary {
                        background: ${if (isDark) "#2b2b2b" else "#f7f7f7"};
                        border: 1px solid ${if (isDark) "#404040" else "#e0e0e0"};
                        border-radius: 4px;
                        padding: 12px;
                        margin: 12px 0;
                        color: ${if (isDark) "#cccccc" else "#333333"};
                    }
                </style>
            </head>
            <body>
                ${processSearchReplaceBlocks(html)}
            </body>
            </html>
        """.trimIndent()
    }

    private fun processSearchReplaceBlocks(html: String): String {
        return html.replace(
            Regex("""(?s)<pre><code>(.+?)<<<<<<< SEARCH\n(.+?)=======\n(.+?)>>>>>>> REPLACE\n</code></pre>"""),
            { matchResult ->
                val (_, filePath, searchBlock, replaceBlock) = matchResult.groupValues
                """
                <div style="font-family: monospace; padding: 4px 0; margin-top: 16px;">$filePath</div>
                <pre>
                <code class="search-block">$searchBlock</code>
                <div style="background: ${if (!com.intellij.ui.JBColor.isBright()) "#666666" else "#cccccc"}; height: 1px;"></div>
                <code class="replace-block">$replaceBlock</code>
                </pre>
                """.trimIndent()
            }
        )
    }

}
