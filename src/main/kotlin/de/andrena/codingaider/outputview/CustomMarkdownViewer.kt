package de.andrena.codingaider.outputview

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.ext.tables.TablesExtension
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
        set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
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

    fun setMarkdownContent(markdown: String) {
        if (markdown == currentContent) return
        currentContent = markdown
        
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        val styledHtml = """
            <html>
            <head>
                <style type="text/css">
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 20px;
                        line-height: 1.6;
                    }
                    pre { 
                        background-color: #2b2b2b; 
                        padding: 10px; 
                        border: 1px solid #444;
                        color: #a9b7c6;
                    }
                    code { 
                        font-family: "JetBrains Mono", "Courier New", Courier, monospace;
                        color: #a9b7c6;
                    }
                    a { 
                        color: #0000EE; 
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
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: #f5f5f5;
                    }
                    tr:nth-child(even) {
                        background-color: #f9f9f9;
                    }
                </style>
            </head>
            <body>
                $html
            </body>
            </html>
        """.trimIndent()

        // Store current caret position
        val caretPos = component.caretPosition
        component.text = styledHtml
        // Restore caret position if possible
        try {
            component.caretPosition = minOf(caretPos, component.document.length)
        } catch (e: Exception) {
            // Ignore if we can't restore position
        }
    }
}


