package de.andrena.codingaider.outputview.markdown

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.UIUtil
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Markdown renderer implementation using JetBrains JCEF (Chromium Embedded Framework)
 */
class JcefMarkdownRenderer(
    private val contentProcessor: MarkdownContentProcessor,
    private val themeManager: MarkdownThemeManager
) : MarkdownRenderer {
    private val logger = Logger.getInstance(JcefMarkdownRenderer::class.java)
    private var isDisposed = false
    private val isInitialized = AtomicBoolean(false)
    private val isLoadCompleted = AtomicBoolean(false)
    
    // Main UI components
    private val mainPanel = JPanel(BorderLayout())
    private val browser = JBCefBrowser()
    
    // JavaScript communication
    private val jsQuery = JBCefJSQuery.create(browser)
    
    // Content state
    private var currentContent = ""
    private var pendingContent: String? = null
    private var pendingThemeUpdate = false
    
    init {
        try {
            setupBrowser()
            setupJavaScriptBridge()
            loadInitialContent()
        } catch (e: Exception) {
            logger.error("Error initializing JCEF renderer", e)
            isInitialized.set(false)
        }
    }
    
    private fun setupBrowser() {
        // Add browser to panel
        mainPanel.add(browser.component, BorderLayout.CENTER)
        
        // Set up load handler to track when page is fully loaded
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                if (!isLoading && !isLoadCompleted.getAndSet(true)) {
                    // Page has finished loading for the first time
                    isInitialized.set(true)
                    
                    // Apply any pending content that was set before initialization completed
                    pendingContent?.let { content ->
                        SwingUtilities.invokeLater {
                            updateContent(content)
                            pendingContent = null
                        }
                    }
                    
                    // Apply any pending theme update
                    if (pendingThemeUpdate) {
                        SwingUtilities.invokeLater {
                            updateTheme(themeManager.isDarkTheme)
                            pendingThemeUpdate = false
                        }
                    }
                }
            }
        }, browser.cefBrowser)
    }
    
    private fun setupJavaScriptBridge() {
        // Create a JavaScript query handler for communication from JS to Java
        jsQuery.addHandler { message ->
            when (message) {
                "ready" -> {
                    isInitialized.set(true)
                    null
                }
                "scrolled" -> {
                    // Handle scroll events from JavaScript if needed
                    null
                }
                else -> {
                    logger.info("Received message from JavaScript: $message")
                    null
                }
            }
        }
    }
    
    private fun loadInitialContent() {
        // Load the initial HTML with the theme manager's base template
        val baseHtml = themeManager.createBaseHtml()
        browser.loadHTML(baseHtml)
        
        // Schedule a check to ensure initialization completes
        Timer().schedule(object : TimerTask() {
            override fun run() {
                if (!isInitialized.get() && !isDisposed) {
                    // Force initialization if it hasn't happened after timeout
                    SwingUtilities.invokeLater {
                        isInitialized.set(true)
                        isLoadCompleted.set(true)
                        
                        // Process any pending content
                        pendingContent?.let { content ->
                            updateContent(content)
                            pendingContent = null
                        }
                    }
                }
            }
        }, 2000) // 2 second timeout
    }
    
    override val component: JComponent
        get() = mainPanel
    
    override val isReady: Boolean
        get() = isInitialized.get() && !isDisposed
    
    override fun setMarkdown(markdown: String) {
        if (isDisposed) {
            return
        }
        
        currentContent = markdown
        
        if (!isReady) {
            // Store content to apply once initialization is complete
            pendingContent = markdown
            return
        }
        
        updateContent(markdown)
    }
    
    private fun updateContent(markdown: String) {
        try {
            // Process markdown to HTML
            val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)
            
            // Use JavaScript to update the content
            val escapedHtml = html.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
            
            // Try both update methods for reliability
            browser.cefBrowser.executeJavaScript(
                "try { updateContent('$escapedHtml'); } catch(e) { console.error(e); }",
                browser.cefBrowser.url, 0
            )
            
            // Also try direct update as fallback
            browser.cefBrowser.executeJavaScript(
                "try { document.getElementById('content').innerHTML = '$escapedHtml'; } catch(e) { console.error(e); }",
                browser.cefBrowser.url, 0
            )
        } catch (e: Exception) {
            logger.error("Error updating content in JCEF renderer", e)
        }
    }
    
    override fun setDarkTheme(isDarkTheme: Boolean) {
        if (isDisposed) {
            return
        }
        
        if (themeManager.updateTheme(isDarkTheme)) {
            if (!isReady) {
                pendingThemeUpdate = true
                return
            }
            
            updateTheme(isDarkTheme)
            
            // Re-render current content with new theme
            if (currentContent.isNotEmpty()) {
                updateContent(currentContent)
            }
        }
    }
    
    private fun updateTheme(isDarkTheme: Boolean) {
        try {
            // Update CSS variables for theme
            val themeScript = """
                document.body.style.backgroundColor = '${if (isDarkTheme) "#2b2b2b" else "#ffffff"}';
                document.body.style.color = '${if (isDarkTheme) "#ffffff" else "#000000"}';
            """.trimIndent()
            
            browser.cefBrowser.executeJavaScript(themeScript, browser.cefBrowser.url, 0)
        } catch (e: Exception) {
            logger.error("Error updating theme in JCEF renderer", e)
        }
    }
    
    override fun scrollToBottom() {
        if (!isReady || isDisposed) {
            return
        }
        
        try {
            // Execute JavaScript to scroll to bottom
            browser.cefBrowser.executeJavaScript(
                "try { scrollToBottom(); } catch(e) { window.scrollTo(0, document.body.scrollHeight); }",
                browser.cefBrowser.url, 0
            )
        } catch (e: Exception) {
            logger.error("Error scrolling to bottom in JCEF renderer", e)
        }
    }
    
    override fun showDevTools(): Boolean {
        if (!isReady || isDisposed) {
            return false
        }
        
        try {
            browser.openDevtools()
            return true
        } catch (e: Exception) {
            logger.error("Error opening DevTools", e)
            return false
        }
    }
    
    override fun supportsDevTools(): Boolean = true
    
    override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            try {
                jsQuery.dispose()
                browser.dispose()
                mainPanel.removeAll()
            } catch (e: Exception) {
                logger.error("Error disposing JCEF renderer", e)
            }
        }
    }
}
