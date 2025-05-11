package de.andrena.codingaider.outputview.markdown

import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Fallback markdown renderer using JEditorPane when JCEF is not available
 */
class FallbackMarkdownRenderer(
    private val contentProcessor: MarkdownContentProcessor,
    private val themeManager: MarkdownThemeManager
) : MarkdownRenderer {

    private val mainPanel = JPanel(BorderLayout())
    private val editorPane = JEditorPane().apply {
        contentType = "text/html; charset=UTF-8"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        putClientProperty("JEditorPane.honorDisplayProperties", true)
        putClientProperty("html.disable", false)
        putClientProperty(JEditorPane.W3C_LENGTH_UNITS, true)
    }

    private var currentContent = ""

    override val component: JComponent
        get() = mainPanel

    override val isReady: Boolean
        get() = true

    init {
        mainPanel.add(editorPane, BorderLayout.CENTER)
    }

    override fun setMarkdown(markdown: String) {
        currentContent = markdown
        updateContent(markdown)
    }

    private fun updateContent(markdown: String) {
        val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)

        SwingUtilities.invokeLater {
            editorPane.putClientProperty("charset", StandardCharsets.UTF_8.name())
            editorPane.text = html
            editorPane.caretPosition = 0
        }
    }

    override fun setDarkTheme(isDarkTheme: Boolean) {
        if (themeManager.updateTheme(isDarkTheme) && currentContent.isNotEmpty()) {
            updateContent(currentContent)
        }
    }
}
