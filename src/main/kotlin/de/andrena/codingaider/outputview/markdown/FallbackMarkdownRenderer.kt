package de.andrena.codingaider.outputview.markdown

import java.awt.BorderLayout
import java.awt.Point
import java.nio.charset.StandardCharsets
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.text.DefaultCaret

/**
 * Fallback markdown renderer using JEditorPane when JCEF is not available
 */
class FallbackMarkdownRenderer(
    private val contentProcessor: MarkdownContentProcessor,
    private val themeManager: MarkdownThemeManager
) : MarkdownRenderer {
    private var isDisposed = false

    private val mainPanel = JPanel(BorderLayout())
    private val editorPane = JEditorPane().apply {
        contentType = "text/html; charset=UTF-8"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        putClientProperty("JEditorPane.honorDisplayProperties", true)
        putClientProperty("html.disable", false)
        putClientProperty(JEditorPane.W3C_LENGTH_UNITS, true)
        
        (caret as? DefaultCaret)?.updatePolicy = DefaultCaret.NEVER_UPDATE
    }
    
    private val scrollPane = JScrollPane(editorPane).apply {
        border = null
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }

    private var currentContent = ""
    private var shouldAutoScroll = true
    private var programmaticScrolling = false
    private var lastScrollPosition: Point? = null
    private var wasAtBottom = true

    override val component: JComponent
        get() = mainPanel

    override val isReady: Boolean
        get() = true

    init {
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        
        // Track user scrolling to determine auto-scroll behavior
        scrollPane.verticalScrollBar.addAdjustmentListener { e ->
            if (!programmaticScrolling && e.valueIsAdjusting) {
                val scrollBar = scrollPane.verticalScrollBar
                val isNearBottom = scrollBar.value >= (scrollBar.maximum - scrollBar.visibleAmount - 20)
                shouldAutoScroll = isNearBottom
            }
        }
    }

    override fun setMarkdown(markdown: String) {
        if (isDisposed) {
            return
        }
        
        // Save scroll state before updating content
        saveScrollState()
        
        currentContent = markdown
        updateContent(markdown)
    }

    private fun saveScrollState() {
        if (isDisposed) {
            return
        }
        
        try {
            // Save current scroll position
            lastScrollPosition = scrollPane.viewport.viewPosition
            
            // Check if we're at the bottom
            val scrollBar = scrollPane.verticalScrollBar
            wasAtBottom = scrollBar.value >= (scrollBar.maximum - scrollBar.visibleAmount - 20)
        } catch (e: Exception) {
            println("Error saving scroll state: ${e.message}")
        }
    }

    private fun updateContent(markdown: String) {
        if (isDisposed) {
            return
        }
        
        val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)

        SwingUtilities.invokeLater {
            try {
                editorPane.putClientProperty("charset", StandardCharsets.UTF_8.name())
                val safeHtml = html.replace("color: #2b2b2b;", "color: #bbbbbb;")
                editorPane.text = safeHtml
                restoreScrollPosition()
            } catch (e: Exception) {
                println("Error updating content in FallbackMarkdownRenderer: ${e.message}")
            }
        }
    }
    
    private fun restoreScrollPosition() {
        // Schedule multiple scroll attempts with increasing delays
        // This helps ensure proper scrolling even with dynamic content
        for (delay in listOf(50L, 150L, 300L)) {
            java.util.Timer().schedule(object : java.util.TimerTask() {
                override fun run() {
                    SwingUtilities.invokeLater {
                        try {
                            if (isDisposed) return@invokeLater
                            
                            programmaticScrolling = true
                            
                            if (shouldAutoScroll || wasAtBottom) {
                                // Scroll to bottom
                                val doc = editorPane.document
                                val rect = editorPane.modelToView(doc.length)
                                if (rect != null) {
                                    rect.y = rect.y + rect.height
                                    editorPane.scrollRectToVisible(rect)
                                } else {
                                    // Fallback method
                                    editorPane.caretPosition = editorPane.document.length
                                }
                            } else if (lastScrollPosition != null) {
                                // Restore previous position
                                scrollPane.viewport.viewPosition = lastScrollPosition
                            }
                            
                            // Only reset the flag on the last timer
                            if (delay == 300L) {
                                programmaticScrolling = false
                            }
                        } catch (e: Exception) {
                            programmaticScrolling = false
                            println("Error restoring scroll position: ${e.message}")
                        }
                    }
                }
            }, delay)
        }
    }

    override fun setDarkTheme(isDarkTheme: Boolean) {
        if (isDisposed) {
            return
        }
        
        // Save scroll state before theme change
        saveScrollState()
        
        if (themeManager.updateTheme(isDarkTheme) && currentContent.isNotEmpty()) {
            updateContent(currentContent)
        }
    }
    
    override fun scrollToBottom() {
        if (isDisposed) {
            return
        }
        
        SwingUtilities.invokeLater {
            try {
                programmaticScrolling = true
                
                // Try to scroll to the end of the document
                val doc = editorPane.document
                editorPane.caretPosition = doc.length
                
                // Also try scrolling to the bottom using rectangle
                val rect = editorPane.modelToView(doc.length)
                if (rect != null) {
                    rect.y = rect.y + rect.height
                    editorPane.scrollRectToVisible(rect)
                }
                
                programmaticScrolling = false
            } catch (e: Exception) {
                programmaticScrolling = false
                println("Error scrolling to bottom: ${e.message}")
            }
        }
    }
    
    override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            try {
                mainPanel.removeAll()
                editorPane.text = ""
            } catch (e: Exception) {
                println("Error disposing FallbackMarkdownRenderer: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun supportsDevTools(): Boolean {
        TODO("Not yet implemented")
    }

    override fun showDevTools(): Boolean {
        TODO("Not yet implemented")
    }
}
