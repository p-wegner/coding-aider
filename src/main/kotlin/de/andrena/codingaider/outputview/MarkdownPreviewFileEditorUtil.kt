package de.andrena.codingaider.outputview

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
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
    private val options = MutableDataSet()
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
                <style>
                    body { font-family: sans-serif; margin: 20px; }
                    pre { background-color: #f5f5f5; padding: 10px; border-radius: 5px; }
                    code { font-family: monospace; }
                    a { color: #2196F3; }
                    img { max-width: 100%; }
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


