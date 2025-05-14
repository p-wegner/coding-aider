package de.andrena.codingaider.outputview.markdown

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import org.cef.handler.CefLoadHandler
import org.cef.browser.CefBrowser
import org.cef.network.CefRequest
import org.cef.browser.CefFrame
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Markdown renderer implementation using JCEF (Chromium Embedded Framework)
 */
class JcefMarkdownRenderer(
    private val contentProcessor: MarkdownContentProcessor,
    private val themeManager: MarkdownThemeManager
) : MarkdownRenderer {
    private var isDisposed = false

    private val mainPanel = JPanel(BorderLayout()).apply {
        border = null
        minimumSize = Dimension(200, 100)
        isOpaque = true
    }

    private var jbCefBrowser: JBCefBrowser? = null
    private var currentContent = ""
    private val pendingMarkdown = mutableListOf<String>()
    private var contentReady = false

    override val component: JComponent
        get() = mainPanel

    override val isReady: Boolean
        get() = contentReady

    init {
        initializeJcefBrowser()
    }

    private fun initializeJcefBrowser() {
        try {
            // Check if JCEF is properly supported before initializing
            if (!JBCefApp.isSupported()) {
                throw IllegalStateException("JCEF is not supported on this platform")
            }
            
            // Clean up any existing browser instance
            if (jbCefBrowser != null) {
                try {
                    mainPanel.remove(jbCefBrowser!!.component)
                    jbCefBrowser!!.jbCefClient.dispose()
                    jbCefBrowser = null
                } catch (e: Exception) {
                    println("Error cleaning up existing browser: ${e.message}")
                }
            }
            
            try {
                jbCefBrowser = JBCefBrowser().apply {
                    component.apply {
                        isFocusable = true
                        minimumSize = Dimension(200, 100)
                    }
    
                    // Load the initial HTML template with a unique URL to avoid caching issues
                    val timestamp = System.currentTimeMillis()
                    loadHTML(themeManager.createBaseHtml(), "http://aider.local/?t=$timestamp")
    
                    // Set a load handler
                    val client: JBCefClient = this.jbCefClient
                    client.addLoadHandler(object : CefLoadHandler {
                        override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                            // Only act on main frame (main frame has no parent)
                            if (frame != null && frame.parent == null) {
                                contentReady = true
                                // Process any pending content with a slight delay to ensure the page is fully rendered
                                pendingMarkdown.lastOrNull()?.let { content ->
                                    SwingUtilities.invokeLater {
                                        try {
                                            // Small delay to ensure browser is fully initialized
                                            javax.swing.Timer(100, { _ ->
                                                try {
                                                    updateContent(content)
                                                } catch (e: Exception) {
                                                    println("Error updating content after page load: ${e.message}")
                                                }
                                            }).apply {
                                                isRepeats = false
                                                start()
                                            }
                                        } catch (e: Exception) {
                                            println("Error scheduling content update: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
    
                        override fun onLoadStart(
                            browser: CefBrowser?,
                            frame: CefFrame?,
                            transitionType: CefRequest.TransitionType?
                        ) {
                            // Not needed
                        }
    
                        override fun onLoadError(
                            browser: CefBrowser?,
                            frame: CefFrame?,
                            errorCode: CefLoadHandler.ErrorCode?,
                            errorText: String?,
                            failedUrl: String?
                        ) {
                            println("JCEF load error: $errorText (code: $errorCode, URL: $failedUrl)")
                            // Don't mark as ready if there was an error loading the initial page
                            contentReady = false
                        }
    
                        override fun onLoadingStateChange(
                            browser: CefBrowser?,
                            isLoading: Boolean,
                            canGoBack: Boolean,
                            canGoForward: Boolean
                        ) {
                            // When loading completes, mark as ready
                            if (!isLoading) {
                                contentReady = true
                            }
                        }
                    }, this.cefBrowser)
                }
    
                mainPanel.add(jbCefBrowser!!.component, BorderLayout.CENTER)
                mainPanel.revalidate()
                mainPanel.repaint()
            } catch (e: NoClassDefFoundError) {
                // This can happen if JCEF classes are missing at runtime
                throw IllegalStateException("JCEF classes not available: ${e.message}")
            } catch (e: UnsatisfiedLinkError) {
                // This can happen if native libraries are missing
                throw IllegalStateException("JCEF native libraries not available: ${e.message}")
            }
        } catch (e: Exception) {
            // Log error but don't throw - the fallback renderer will be used instead
            println("Error initializing JCEF browser: ${e.message}")
            e.printStackTrace()
            contentReady = false
        }
    }

    override fun setMarkdown(markdown: String) {
        if (isDisposed) {
            return
        }
        
        currentContent = markdown

        if (!contentReady) {
            // just remember it
            pendingMarkdown.clear()
            pendingMarkdown += markdown
            return
        }

        updateContent(markdown)
    }

    private fun updateContent(markdown: String) {
        if (isDisposed) {
            return
        }
        
        val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)

        jbCefBrowser?.let { browser ->
            try {
                // Check if browser is properly initialized
                if (browser.cefBrowser == null) {
                    println("Cannot update content: CEF browser is null")
                    // Try to reinitialize the browser
                    initializeJcefBrowser()
                    return
                }
                
                // First try direct HTML loading which is more reliable
                try {
                    browser.loadHTML(themeManager.createHtmlWithContent(html), "http://aider.local/")
                    return
                } catch (e: Exception) {
                    println("Failed direct HTML loading, falling back to JavaScript: ${e.message}")
                }
                
                // Fallback: Use JavaScript to update the content
                val escapedHtml = org.apache.commons.text
                    .StringEscapeUtils.escapeEcmaScript(html)
                val script = "try { updateContent('$escapedHtml'); } catch(e) { document.getElementById('content').innerHTML = '$escapedHtml'; }"
                
                try {
                    browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
                } catch (e: Exception) {
                    println("Error during JavaScript execution: ${e.message}")
                    // Last resort: full page reload
                    try {
                        browser.loadHTML(themeManager.createHtmlWithContent(html), "http://aider.local/")
                    } catch (e2: Exception) {
                        println("Failed all content update methods: ${e2.message}")
                    }
                }
            } catch (e: Exception) {
                println("Error accessing browser: ${e.message}")
            }
        } ?: run {
            // Browser is null, try to reinitialize
            println("Browser is null, attempting to reinitialize")
            initializeJcefBrowser()
        }
    }

    override fun setDarkTheme(isDarkTheme: Boolean) {
        if (isDisposed) {
            return
        }
        
        if (themeManager.updateTheme(isDarkTheme) && currentContent.isNotEmpty()) {
            // Reload with new theme
            jbCefBrowser?.loadHTML(themeManager.createBaseHtml())
            setMarkdown(currentContent)
        }
    }
    
    override fun supportsDevTools(): Boolean {
        return !isDisposed && jbCefBrowser != null
    }
    
    override fun showDevTools(): Boolean {
        if (isDisposed || jbCefBrowser == null) {
            return false
        }
        
        try {
            // Check if browser is properly initialized
            if (jbCefBrowser?.cefBrowser == null) {
                println("Cannot show DevTools: CEF browser is null")
                return false
            }
            
            jbCefBrowser?.openDevtools()
            return true
        } catch (e: NullPointerException) {
            println("NullPointerException showing DevTools: ${e.message}")
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            println("Error showing DevTools: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            try {
                jbCefBrowser?.let { browser ->
                    browser.jbCefClient.dispose()
                    mainPanel.removeAll()
                }
                jbCefBrowser = null
            } catch (e: Exception) {
                println("Error disposing JcefMarkdownRenderer: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
