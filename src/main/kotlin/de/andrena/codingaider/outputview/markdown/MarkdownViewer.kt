package de.andrena.codingaider.outputview.markdown

import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * A refactored Markdown viewer component that uses JCEF (Chromium Embedded Framework)
 * with fallback to JEditorPane when JCEF is not available.
 */
class MarkdownViewer(private val lookupPaths: List<String> = emptyList()) {

    private val mainPanel = JPanel(BorderLayout()).apply {
        border = null
        minimumSize = Dimension(200, 100)
        preferredSize = Dimension(600, 400)
        isOpaque = true
        background = if (!JBColor.isBright()) JBColor(0x2B2B2B, 0x2B2B2B) else JBColor.WHITE
    }

    private val themeManager = MarkdownThemeManager()
    private val contentProcessor = MarkdownContentProcessor(lookupPaths)
    private val renderer: MarkdownRenderer

    // State tracking
    private var currentContent = ""

    init {
        renderer = createRenderer()
        mainPanel.add(renderer.component, BorderLayout.CENTER)
    }

    private fun createRenderer(): MarkdownRenderer {
        return try {
            if (JBCefApp.isSupported()) {
                JcefMarkdownRenderer(contentProcessor, themeManager)
            } else {
                FallbackMarkdownRenderer(contentProcessor, themeManager)
            }
        } catch (e: Exception) {
            println("Error initializing markdown renderer: ${e.message}")
            e.printStackTrace()
            FallbackMarkdownRenderer(contentProcessor, themeManager)
        }
    }

    val component: JComponent
        get() = mainPanel

    /**
     * Sets the markdown content to be displayed
     */
    fun setMarkdown(markdown: String) {
        currentContent = markdown
        renderer.setMarkdown(markdown)
    }

    /**
     * Updates the theme used for rendering
     */
    fun setDarkTheme(dark: Boolean) {
        renderer.setDarkTheme(dark)
    }
}
