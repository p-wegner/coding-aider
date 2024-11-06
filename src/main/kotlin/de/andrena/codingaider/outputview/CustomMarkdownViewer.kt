package de.andrena.codingaider.outputview

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import java.awt.Desktop
import java.net.URI
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

class CustomMarkdownViewer {
    val component: JEditorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        putClientProperty("JEditorPane.honorDisplayProperties", true)
    }
    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create()
        ))
    }
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    init {
        component.addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(URI(event.url.toString()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private var currentContent = ""
    private var isDarkTheme = false

    companion object {
        private fun getHtmlTemplate(
            bodyBg: String,
            bodyText: String,
            preBg: String,
            preBorder: String,
            preText: String,
            codeColor: String,
            linkColor: String,
            tableBorder: String,
            thBg: String,
            trEvenBg: String,
            content: String
        ): String = """
            <html>
            <head>
                <style type="text/css">
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 20px;
                        line-height: 1.6;
                        background-color: $bodyBg;
                        color: $bodyText;
                    }
                    pre { 
                        background-color: $preBg; 
                        padding: 10px; 
                        border: 1px solid $preBorder;
                        color: $preText;
                    }
                    code { 
                        font-family: "JetBrains Mono", "Courier New", Courier, monospace;
                        color: $codeColor;
                    }
                    a { 
                        color: $linkColor; 
                        text-decoration: underline;
                    }
                    img { 
                        width: auto;
                        height: auto;
                        max-width: 100%;
                    }
                    body {
                        word-wrap: break-word;
                    }
                    pre {
                        white-space: pre;
                        overflow-x: auto;
                    }
                    table {
                        border-collapse: collapse;
                        margin: 15px 0;
                        width: 100%;
                    }
                    th, td {
                        border: 1px solid $tableBorder;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: $thBg;
                    }
                    tr:nth-child(even) {
                        background-color: $trEvenBg;
                    }
                </style>
            </head>
            <body>
                $content
            </body>
            </html>
        """
    }

    fun setDarkTheme(dark: Boolean) {
        isDarkTheme = dark
        if (currentContent.isNotEmpty()) {
            setMarkdownContent(currentContent)
        }
    }

    fun setMarkdownContent(markdown: String) {
        if (markdown == currentContent) return
        currentContent = markdown
        
        try {
            val document = parser.parse(markdown)
            val html = renderer.render(document)
            
            val colors = if (isDarkTheme) {
                arrayOf(
                    "#2b2b2b", // body background
                    "#ffffff", // body text
                    "#1e1e1e", // pre background
                    "#666666", // pre border
                    "#ffffff", // pre/code text
                    "#ffffff", // code color
                    "#589df6", // link color
                    "#666666", // table border
                    "#323232", // th background
                    "#363636"  // tr even background
                )
            } else {
                arrayOf(
                    "#ffffff", // body background
                    "#000000", // body text
                    "#f5f5f5", // pre background
                    "#cccccc", // pre border
                    "#000000", // pre/code text
                    "#000000", // code color
                    "#0066cc", // link color
                    "#cccccc", // table border
                    "#e6e6e6", // th background
                    "#f5f5f5"  // tr even background
                )
            }

            val styledHtml = getHtmlTemplate(
                bodyBg = colors[0],
                bodyText = colors[1],
                preBg = colors[2],
                preBorder = colors[3],
                preText = colors[4],
                codeColor = colors[5],
                linkColor = colors[6],
                tableBorder = colors[7],
                thBg = colors[8],
                trEvenBg = colors[9],
                content = html
            )

            // Store current caret position
            val caretPos = component.caretPosition
            component.text = styledHtml
            // Restore caret position if possible
            try {
                component.caretPosition = minOf(caretPos, component.document.length)
            } catch (e: Exception) {
                // Ignore if we can't restore position
            }
        } catch (e: Exception) {
            component.text = """
                <html>
                <body>
                <h1>Error Rendering Markdown</h1>
                <p>There was an error processing the markdown content:</p>
                <pre>${e.message}</pre>
                </body>
                </html>
            """.trimIndent()
        }
    }
}

