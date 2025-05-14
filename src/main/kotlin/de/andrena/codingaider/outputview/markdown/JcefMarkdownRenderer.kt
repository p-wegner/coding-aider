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
            
            try {
                jbCefBrowser = JBCefBrowser().apply {
                    component.apply {
                        isFocusable = true
                        minimumSize = Dimension(200, 100)
                    }
    
                    // Load the initial HTML template
                    loadHTML(themeManager.createBaseHtml(), "http://aider.local/")
    
                    // Set a load handler
                    val client: JBCefClient = this.jbCefClient
                    client.addLoadHandler(object : CefLoadHandler {
                        override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                            // Only act on main frame (main frame has no parent)
                            if (frame != null && frame.parent == null) {
                                contentReady = true
                                // Paint whatever arrived before the page was ready
                                pendingMarkdown.lastOrNull()?.let { 
                                    SwingUtilities.invokeLater { 
                                        try {
                                            updateContent(it)
                                        } catch (e: Exception) {
                                            println("Error updating content after page load: ${e.message}")
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
                        }
    
                        override fun onLoadingStateChange(
                            browser: CefBrowser?,
                            isLoading: Boolean,
                            canGoBack: Boolean,
                            canGoForward: Boolean
                        ) {
                            // Not needed
                        }
                    }, this.cefBrowser)
                }
    
                mainPanel.add(jbCefBrowser!!.component, BorderLayout.CENTER)
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
                    return
                }
                
                // Use JavaScript to update the content
                val escapedHtml = org.apache.commons.text
                    .StringEscapeUtils.escapeEcmaScript(html)
                val script = "updateContent('$escapedHtml');"
                
                try {
                    browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
                } catch (e: NullPointerException) {
                    println("NullPointerException during JavaScript execution: ${e.message}")
                    // Fallback to full page reload if JavaScript execution fails due to NPE
                    try {
                        browser.loadHTML(themeManager.createHtmlWithContent(html))
                    } catch (e2: Exception) {
                        println("Failed to load HTML content: ${e2.message}")
                    }
                } catch (e: Exception) {
                    println("Error updating browser content: ${e.message}")
                    // Fallback to full page reload if JavaScript execution fails
                    try {
                        browser.loadHTML(themeManager.createHtmlWithContent(html))
                    } catch (e2: Exception) {
                        println("Failed to load HTML content: ${e2.message}")
                    }
                }
            } catch (e: Exception) {
                println("Error accessing browser: ${e.message}")
            }
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
