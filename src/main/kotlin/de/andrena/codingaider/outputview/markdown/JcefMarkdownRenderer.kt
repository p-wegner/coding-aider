package de.andrena.codingaider.outputview.markdown

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
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
    private var jsQuery: JBCefJSQuery? = null
    private var jsQueryInitialized = false
    
    // Content state
    private var currentContent = ""
    private var pendingContent: String? = null
    private var pendingThemeUpdate = false
    private var jsErrorCount = 0
    
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
        try {
            // Create a JavaScript query handler for communication from JS to Java
            // Wrap in try-catch to handle potential NullPointerException in JBCefJSQuery.create
            try {
                jsQuery = JBCefJSQuery.create(browser)
            } catch (e: Exception) {
                logger.warn("Failed to create JBCefJSQuery object: ${e.message}", e)
                jsQuery = null
            }
            
            // Verify the query object was created successfully
            if (jsQuery == null) {
                logger.warn("JavaScript bridge unavailable - continuing in limited mode")
                isInitialized.set(true)
                jsQueryInitialized = false
                return
            }
            
            // Try to add handler with additional error checking
            try {
                jsQuery?.addHandler { message ->
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
                jsQueryInitialized = true
            } catch (e: Exception) {
                logger.warn("Failed to add handler to JavaScript bridge: ${e.message}", e)
                jsQueryInitialized = false
            }
        } catch (e: Exception) {
            logger.warn("Failed to initialize JavaScript bridge: ${e.message}", e)
            // Mark as initialized anyway so we can continue without JS communication
            isInitialized.set(true)
            jsQueryInitialized = false
        }
    }
    
    private fun loadInitialContent() {
        try {
            // Load the initial HTML with the theme manager's base template
            val baseHtml = themeManager.createBaseHtml()
            browser.loadHTML(baseHtml)
            
            // Add a JavaScript callback to notify when the page is ready
            if (jsQueryInitialized && jsQuery != null) {
                try {
                    // Use a safer approach to inject the JavaScript callback
                    val jsCallbackCode = if (jsQuery != null && jsQueryInitialized) {
                        // Create a safe version of the inject call that won't fail if objId is null
                        val safeInject = try {
                            // Only attempt to get inject code if query is initialized
                            jsQuery?.inject("message") ?: "console.log('JS bridge unavailable:', message)"
                        } catch (e: Exception) {
                            logger.warn("Failed to create inject code, using fallback", e)
                            "console.log('JS bridge error:', message)"
                        }
                        
                        """
                        if (document.readyState === 'complete') {
                            try {
                                window.javaCallback = function(message) {
                                    try { ${safeInject} } catch(e) { console.error('Callback error:', e); }
                                };
                                // Mark as ready even without callback
                                console.log('Markdown viewer ready');
                                setTimeout(function() { 
                                    try { window.javaCallback('ready'); } 
                                    catch(e) { console.error('Ready callback failed:', e); }
                                }, 100);
                            } catch(e) { console.error('Callback setup error:', e); }
                        } else {
                            document.addEventListener('DOMContentLoaded', function() {
                                try {
                                    window.javaCallback = function(message) {
                                        try { ${safeInject} } catch(e) { console.error('Callback error:', e); }
                                    };
                                    // Mark as ready even without callback
                                    console.log('Markdown viewer ready (DOMContentLoaded)');
                                    setTimeout(function() { 
                                        try { window.javaCallback('ready'); } 
                                        catch(e) { console.error('Ready callback failed:', e); }
                                    }, 100);
                                } catch(e) { console.error('Callback setup error:', e); }
                            });
                        }
                        """
                    } else {
                        // Fallback if jsQuery is null or not initialized
                        """
                        console.log('JavaScript bridge not available, using fallback initialization');
                        if (document.readyState === 'complete') {
                            console.log('Document ready - no JS bridge');
                            // Force content element creation to ensure we have somewhere to put content
                            if (!document.getElementById('content')) {
                                const contentDiv = document.createElement('div');
                                contentDiv.id = 'content';
                                document.body.appendChild(contentDiv);
                                console.log('Created content element');
                            }
                        } else {
                            document.addEventListener('DOMContentLoaded', function() {
                                console.log('Document ready (event) - no JS bridge');
                                // Force content element creation
                                if (!document.getElementById('content')) {
                                    const contentDiv = document.createElement('div');
                                    contentDiv.id = 'content';
                                    document.body.appendChild(contentDiv);
                                    console.log('Created content element (DOMContentLoaded)');
                                }
                            });
                        }
                        """
                    }
                    
                    try {
                        browser.cefBrowser.executeJavaScript(jsCallbackCode.trimIndent(), browser.cefBrowser.url, 0)
                    } catch (e: Exception) {
                        logger.warn("Failed to inject JavaScript callback: ${e.message}", e)
                        // Mark as initialized anyway so we can continue without JS communication
                        isInitialized.set(true)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to prepare JavaScript callback: ${e.message}", e)
                    isInitialized.set(true)
                }
            } else {
                // If JS bridge failed to initialize, mark as initialized anyway
                isInitialized.set(true)
            }
            
            // Force a small initial content to ensure the page is properly initialized
            try {
                browser.cefBrowser.executeJavaScript(
                    """
                    try { 
                        // Create content element if it doesn't exist
                        if (!document.getElementById('content')) {
                            const contentDiv = document.createElement('div');
                            contentDiv.id = 'content';
                            document.body.appendChild(contentDiv);
                        }
                        document.getElementById('content').innerHTML = '<p>Initializing...</p>'; 
                    } catch(e) { 
                        console.error('Init error:', e); 
                        // Last resort attempt to create content element
                        try {
                            const contentDiv = document.createElement('div');
                            contentDiv.id = 'content';
                            contentDiv.innerHTML = '<p>Initializing...</p>';
                            document.body.appendChild(contentDiv);
                        } catch(e2) {
                            console.error('Emergency init failed:', e2);
                        }
                    }
                    """,
                    browser.cefBrowser.url, 0
                )
            } catch (e: Exception) {
                logger.warn("Failed to set initial content", e)
            }
        } catch (e: Exception) {
            logger.error("Error in loadInitialContent", e)
            isInitialized.set(true) // Ensure we don't get stuck
        }
        
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
                        
                        // Inject the JavaScript callback again as a fallback
                        if (jsQueryInitialized && jsQuery != null) {
                            try {
                                // Only attempt to inject if JS query is available and initialized
                                if (jsQuery != null && jsQueryInitialized) {
                                    val safeInject = try {
                                        jsQuery?.inject("message") ?: "console.log('Fallback callback:', message)"
                                    } catch (e: Exception) {
                                        "console.log('Error callback:', message)"
                                    }
                                    
                                    try {
                                        browser.cefBrowser.executeJavaScript("""
                                            try {
                                                window.javaCallback = function(message) {
                                                    try { ${safeInject} } catch(e) { console.error('Callback error:', e); }
                                                };
                                                console.log('Fallback callback initialized');
                                            } catch(e) { console.error('Callback setup error:', e); }
                                        """.trimIndent(), browser.cefBrowser.url, 0)
                                    } catch (e: Exception) {
                                        logger.warn("Failed to inject fallback JavaScript callback", e)
                                    }
                                } else {
                                    // If JS bridge isn't available, just make sure content element exists
                                    try {
                                        browser.cefBrowser.executeJavaScript("""
                                            try {
                                                if (!document.getElementById('content')) {
                                                    const contentDiv = document.createElement('div');
                                                    contentDiv.id = 'content';
                                                    document.body.appendChild(contentDiv);
                                                    console.log('Created content element in fallback');
                                                }
                                            } catch(e) { console.error('Fallback content creation error:', e); }
                                        """.trimIndent(), browser.cefBrowser.url, 0)
                                    } catch (e: Exception) {
                                        logger.warn("Failed to ensure content element exists", e)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error("Error injecting JavaScript callback", e)
                            }
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
    
    fun updateContent(markdown: String) {
        try {
            // Process markdown to HTML
            val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)
            
            // Use a more robust method to escape HTML for JavaScript
            val escapedHtml = escapeJavaScriptString(html)
            
            try {
                // First try using directUpdateContent which is more reliable
                val updateScript = """
                    try {
                        if (typeof directUpdateContent === 'function') {
                            directUpdateContent(`$escapedHtml`);
                        } else if (typeof updateContent === 'function') {
                            updateContent(`$escapedHtml`);
                        } else {
                            // Fallback to direct DOM manipulation
                            let contentElement = document.getElementById('content');
                            if (!contentElement) {
                                // Try to create the content element if it doesn't exist
                                console.log('Content element not found, creating it');
                                const bodyElement = document.body;
                                if (bodyElement) {
                                    contentElement = document.createElement('div');
                                    contentElement.id = 'content';
                                    bodyElement.appendChild(contentElement);
                                }
                            }
                            
                            if (contentElement) {
                                contentElement.innerHTML = `$escapedHtml`;
                                // Initialize any collapsible panels
                                if (typeof initCollapsiblePanels === 'function') {
                                    initCollapsiblePanels();
                                } else {
                                    // Basic collapsible panel initialization if function is missing
                                    document.querySelectorAll('.collapsible-panel').forEach(panel => {
                                        const header = panel.querySelector('.collapsible-header');
                                        if (header) {
                                            header.addEventListener('click', function() {
                                                panel.classList.toggle('expanded');
                                                const arrow = this.querySelector('.collapsible-arrow');
                                                if (arrow) {
                                                    arrow.textContent = panel.classList.contains('expanded') ? '▼' : '▶';
                                                }
                                            });
                                        }
                                    });
                                }
                            } else {
                                console.error('Failed to find or create content element');
                                // Last resort - try to append to body directly
                                document.body.innerHTML += `<div id="content">${escapedHtml}</div>`;
                            }
                        }
                    } catch(e) {
                        console.error('Error updating content:', e);
                        // Emergency fallback - replace entire body content
                        try {
                            document.body.innerHTML = `<div id="content">${escapedHtml}</div>`;
                        } catch(e2) {
                            console.error('Emergency content update failed:', e2);
                        }
                    }
                """.trimIndent()
                
                browser.cefBrowser.executeJavaScript(updateScript, browser.cefBrowser.url, 0)
                jsErrorCount = 0 // Reset error count on success
            } catch (e: Exception) {
                jsErrorCount++
                logger.warn("Error executing JavaScript (attempt $jsErrorCount): ${e.message}", e)
                
                if (jsErrorCount > 2) {
                    // After multiple failures, try reloading the page with the content directly
                    try {
                        val fullHtml = themeManager.createHtmlWithContent(html)
                        browser.loadHTML(fullHtml)
                        jsErrorCount = 0 // Reset after reload attempt
                    } catch (reloadEx: Exception) {
                        logger.error("Failed to reload page with content", reloadEx)
                        
                        // Last resort: try to create a completely new browser instance
                        if (jsErrorCount > 5) {
                            try {
                                // Force a complete reload with minimal HTML
                                browser.loadHTML("<html><body><div id='content'>Loading...</div></body></html>")
                                
                                // After a short delay, try to update the content again
                                java.util.Timer().schedule(object : java.util.TimerTask() {
                                    override fun run() {
                                        try {
                                            browser.cefBrowser.executeJavaScript(
                                                "document.getElementById('content').innerHTML = `$escapedHtml`;",
                                                browser.cefBrowser.url, 0
                                            )
                                        } catch (e: Exception) {
                                            logger.error("Failed in last resort content update", e)
                                        }
                                    }
                                }, 500)
                            } catch (e: Exception) {
                                logger.error("Failed in emergency reload", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error updating content in JCEF renderer", e)
        }
    }
    
    /**
     * More robust JavaScript string escaping that handles multi-line content
     */
    private fun escapeJavaScriptString(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\t", "\\t")
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
            logger.warn("Error updating theme in JCEF renderer: ${e.message}")
            
            // If JavaScript execution fails, try reloading the page with the current content
            // and the updated theme
            try {
                if (currentContent.isNotEmpty()) {
                    val html = contentProcessor.processMarkdown(currentContent, isDarkTheme)
                    val fullHtml = themeManager.createHtmlWithContent(html)
                    browser.loadHTML(fullHtml)
                }
            } catch (reloadEx: Exception) {
                logger.error("Failed to reload page with updated theme", reloadEx)
            }
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
            logger.warn("Error scrolling to bottom in JCEF renderer: ${e.message}")
            
            // Try again with a simpler approach
            try {
                browser.cefBrowser.executeJavaScript(
                    "window.scrollTo(0, document.body.scrollHeight);",
                    browser.cefBrowser.url, 0
                )
            } catch (e2: Exception) {
                logger.error("Failed to scroll to bottom with fallback method", e2)
            }
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
                jsQuery?.dispose()
                browser.dispose()
                mainPanel.removeAll()
            } catch (e: Exception) {
                logger.error("Error disposing JCEF renderer", e)
            }
        }
    }
}
