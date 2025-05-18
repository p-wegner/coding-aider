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

        // Try multiple times with increasing delays to handle race conditions
        // where the renderer might not be fully initialized
        try {
            // Initial update attempt
            renderer.setMarkdown(currentContent)

            // Schedule additional attempts with delays to ensure content is displayed
            // Use a more reliable approach with fewer, more strategic attempts
            for (delay in listOf(200L, 500L, 1000L, 2000L)) {
                java.util.Timer().schedule(object : TimerTask() {
                    override fun run() {
                        try {
                            if (!isDisposed) {
                                javax.swing.SwingUtilities.invokeLater {
                                    try {
                                        // Only update if the renderer is ready
                                        if (renderer.isReady) {
                                            renderer.setMarkdown(currentContent)
                                            
                                            // No auto-scrolling
                                        } else if (delay == 2000L) {
                                            // Last attempt - force update even if not ready
                                            println("Forcing markdown update after timeout")
                                            renderer.setMarkdown(currentContent)
                                        }
                                    } catch (e: Exception) {
                                        println("Error in delayed markdown update (${delay}ms): ${e.message}")
                                        e.printStackTrace()
                                        
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("Error scheduling delayed update: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }, delay)
            }
        } catch (e: Exception) {
            println("Error in initial markdown update: ${e.message}")
            e.printStackTrace()
            
            // If initial attempt fails, try one more time after a short delay
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
    
    fun setAutoScroll(isAutoScroll: Boolean) {
        if (isDisposed) {
            return
        }
        
        if (renderer is JcefMarkdownRenderer) {
            renderer.setAutoScroll(isAutoScroll)
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
