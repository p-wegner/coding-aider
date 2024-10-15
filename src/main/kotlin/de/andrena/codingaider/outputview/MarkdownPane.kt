package de.andrena.codingaider.outputview

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import javax.swing.JEditorPane
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

class MarkdownPane : JEditorPane() {
    private val parser: Parser
    private val renderer: HtmlRenderer

    init {
        contentType = "text/html"
        isEditable = false
        
        val options = MutableDataSet()
        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()

        val kit = HTMLEditorKit()
        setEditorKit(kit)

        val styleSheet = StyleSheet()
        styleSheet.addRule("body { font-family: Arial, sans-serif; font-size: 14pt; }")
        styleSheet.addRule("pre { background-color: #f0f0f0; padding: 10px; }")
        styleSheet.addRule("code { font-family: monospace; }")
        kit.styleSheet = styleSheet
    }

    fun setMarkdownText(markdown: String) {
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        text = html
        caretPosition = 0
    }
}
