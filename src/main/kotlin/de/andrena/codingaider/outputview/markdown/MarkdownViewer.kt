package de.andrena.codingaider.outputview.markdown

import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.TimerTask
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
                try {
                    JcefMarkdownRenderer(contentProcessor, themeManager)
                } catch (e: Exception) {
                    println("Error initializing JCEF renderer, falling back to basic renderer: ${e.message}")
                    e.printStackTrace()
                    FallbackMarkdownRenderer(contentProcessor, themeManager)
                }
            } else {
                println("JCEF not supported on this platform, using fallback renderer")
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

        // Simple, direct update - let the smart scrolling in JavaScript handle the rest
        try {
            renderer.setMarkdown(currentContent)
        } catch (e: Exception) {
            println("Error in markdown update: ${e.message}")
            e.printStackTrace()
            
            // Single retry after a short delay
            java.util.Timer().schedule(object : TimerTask() {
                override fun run() {
                    try {
                        if (!isDisposed) {
                            javax.swing.SwingUtilities.invokeLater {
                                try {
                                    renderer.setMarkdown(currentContent)
                                } catch (e: Exception) {
                                    println("Error in recovery markdown update: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error scheduling recovery update: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }, 100L)
        }
    }
    
    fun setDarkTheme(dark: Boolean) {
        if (isDisposed) {
            return
        }
        renderer.setDarkTheme(dark)
    }
    
    fun scrollToBottom() {
        if (isDisposed) {
            return
        }
        renderer.scrollToBottom()
    }
    

    fun supportsDevTools(): Boolean {
        if (isDisposed) {
            return false
        }
        return renderer.supportsDevTools()
    }

    fun showDevTools(): Boolean {
        if (isDisposed) {
            return false
        }
        return renderer.showDevTools()
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
