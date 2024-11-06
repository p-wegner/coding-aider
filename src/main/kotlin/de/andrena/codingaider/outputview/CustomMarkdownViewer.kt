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
        private const val HTML_TEMPLATE = """
            <html>
            <head>
                <style type="text/css">
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 20px;
                        line-height: 1.6;
                        background-color: %s;
                        color: %s;
                    }
                    pre { 
                        background-color: %s; 
                        padding: 10px; 
                        border: 1px solid %s;
                        color: %s;
                    }
                    code { 
                        font-family: "JetBrains Mono", "Courier New", Courier, monospace;
                        color: %s;
                    }
                    a { 
                        color: %s; 
                        text-decoration: underline;
                    }
                    img { 
                        width: auto;
                        height: auto;
                        max-width: 100%;
                    }
                    table {
                        border-collapse: collapse;
                        margin: 15px 0;
                        width: 100%;
                    }
                    th, td {
                        border: 1px solid %s;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: %s;
                    }
                    tr:nth-child(even) {
                        background-color: %s;
                    }
                </style>
            </head>
            <body>
                %s
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
                    "#a9b7c6", // body text
                    "#1e1e1e", // pre background
                    "#444444", // pre border
                    "#a9b7c6", // pre/code text
                    "#a9b7c6", // code color
                    "#589df6", // link color
                    "#444444", // table border
                    "#323232", // th background
                    "#2b2b2b"  // tr even background
                )
            } else {
                arrayOf(
                    "#ffffff", // body background
                    "#000000", // body text
                    "#f5f5f5", // pre background
                    "#e0e0e0", // pre border
                    "#000000", // pre/code text
                    "#000000", // code color
                    "#0000EE", // link color
                    "#ddd",    // table border
                    "#f5f5f5", // th background
                    "#f9f9f9"  // tr even background
                )
            }

            val styledHtml = HTML_TEMPLATE.format(*colors, html)

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


