package de.andrena.codingaider.outputview.markdown

import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class MarkdownViewer(private val lookupPaths: List<String> = emptyList()) {
    private var isDisposed = false

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

    fun setMarkdown(markdown: String) {
        if (isDisposed) {
            return
        }
        // Never feed an empty string to the renderer – give it one nbsp instead
        currentContent = markdown.ifBlank { " " }
        renderer.setMarkdown(currentContent)
    }

    fun setDarkTheme(dark: Boolean) {
        if (isDisposed) {
            return
        }
        renderer.setDarkTheme(dark)
    }
    
    fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            try {
                renderer.dispose()
                mainPanel.removeAll()
            } catch (e: Exception) {
                println("Error disposing MarkdownViewer: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
