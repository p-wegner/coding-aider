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
//                    FallbackMarkdownRenderer(contentProcessor, themeManager)
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

        // Try multiple times with increasing delays to handle race conditions
        // where the renderer might not be fully initialized
        try {
            renderer.setMarkdown(currentContent)

            // Schedule additional attempts with delays to ensure content is displayed
            for (delay in listOf(100L, 300L, 600L)) {
                java.util.Timer().schedule(object : TimerTask() {
                    override fun run() {
                        try {
                            if (!isDisposed) {
                                javax.swing.SwingUtilities.invokeLater {
                                    try {
                                        renderer.setMarkdown(currentContent)
                                        
                                        // For the last attempt, try to scroll to bottom if needed
                                        if (delay == 600L) {
                                            renderer.scrollToBottom()
                                        }
                                    } catch (e: Exception) {
                                        println("Error in delayed markdown update (${delay}ms): ${e.message}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("Error scheduling delayed update: ${e.message}")
                        }
                    }
                }, delay)
            }
        } catch (e: Exception) {
            println("Error in initial markdown update: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun setDarkTheme(dark: Boolean) {
        if (isDisposed) {
            return
        }
        renderer.setDarkTheme(dark)
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
