package de.andrena.codingaider.outputview.markdown

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.concurrency.AppExecutorUtil
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Markdown renderer implementation using JCEF (Chromium Embedded Framework)
 * for high-quality HTML rendering with JavaScript support.
 */
class JcefMarkdownRenderer(
    private val contentProcessor: MarkdownContentProcessor,
    private val themeManager: MarkdownThemeManager
) : MarkdownRenderer {
    private val LOG = Logger.getInstance(JcefMarkdownRenderer::class.java)
    private val isDisposed = AtomicBoolean(false)
    private val browser: JBCefBrowser = JBCefBrowser()
    private val mainPanel = JPanel(BorderLayout())
    private val loadCompleted = AtomicBoolean(false)
    private val pendingContent = AtomicReference<String?>(null)
    private val devToolsInstances = mutableSetOf<CefBrowser>()
    private val parentDisposable = Disposer.newDisposable("JcefMarkdownRenderer")
    
    private var currentContent = ""
    private var initializationFuture: CompletableFuture<Boolean>? = null

    override val component: JComponent
        get() = mainPanel

    override val isReady: Boolean
        get() = loadCompleted.get() && !isDisposed.get()

    init {
        // Configure browser and panel
        browser.component.minimumSize = Dimension(200, 100)
        browser.component.preferredSize = Dimension(600, 400)
        mainPanel.add(browser.component, BorderLayout.CENTER)
        
        // Register browser with parent disposable
        Disposer.register(parentDisposable, browser)
        
        // Listen for theme changes
        themeManager.addThemeChangeListener(parentDisposable) { isDark ->
            if (!isDisposed.get()) {
                SwingUtilities.invokeLater {
                    setDarkTheme(isDark)
                }
            }
        }
        
        // Initialize browser with HTML template
        initializeBrowser()
        
        // Set up load handler to detect when browser is ready
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                if (!isLoading) {
                    // Browser is ready, apply any pending content
                    SwingUtilities.invokeLater {
                        if (loadCompleted.compareAndSet(false, true)) {
                            pendingContent.getAndSet(null)?.let { content ->
                                updateContent(content)
                            }
                            initializationFuture?.complete(true)
                        }
                    }
                }
            }
        }, browser.cefBrowser)
        
        // Register load handler with parent disposable
        Disposer.register(parentDisposable, Disposable {
            try {
                browser.jbCefClient.removeLoadHandler(browser.cefBrowser)
            } catch (e: Exception) {
                LOG.warn("Error removing load handler", e)
            }
        })
        
        // Set up initialization future with timeout
        initializationFuture = CompletableFuture<Boolean>().completeOnTimeout(true, 2, TimeUnit.SECONDS)
        initializationFuture?.thenAcceptAsync({
            SwingUtilities.invokeLater {
                if (loadCompleted.compareAndSet(false, true)) {
                    pendingContent.getAndSet(null)?.let { content ->
                        updateContent(content)
                    }
                }
            }
        }, AppExecutorUtil.getAppExecutorService())
    }

    private fun initializeBrowser() {
        val htmlTemplate = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Markdown Viewer</title>
                <style>
                    body {
                        margin: 0;
                        padding: 0;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        background-color: ${if (themeManager.isDarkTheme) "#2b2b2b" else "#ffffff"};
                        color: ${if (themeManager.isDarkTheme) "#ffffff" else "#000000"};
                        transition: background-color 0.3s ease, color 0.3s ease;
                    }
                    #content {
                        padding: 20px;
                    }
                    
                    /* Collapsible panel styles */
                    .collapsible-panel {
                        margin-bottom: 10px;
                        border: 1px solid rgba(127, 127, 127, 0.2);
                        border-radius: 4px;
                        overflow: hidden;
                    }
                    
                    .collapsible-header {
                        padding: 8px 12px;
                        background-color: rgba(127, 127, 127, 0.1);
                        cursor: pointer;
                        display: flex;
                        align-items: center;
                        user-select: none;
                    }
                    
                    .collapsible-header:hover {
                        background-color: rgba(127, 127, 127, 0.2);
                    }
                    
                    .collapsible-arrow {
                        margin-right: 8px;
                        font-size: 12px;
                        width: 12px;
                        text-align: center;
                    }
                    
                    .collapsible-title {
                        flex-grow: 1;
                        font-weight: 500;
                    }
                    
                    .collapsible-content {
                        padding: 0;
                        max-height: 0;
                        overflow: hidden;
                        transition: max-height 0.3s ease, padding 0.3s ease;
                    }
                    
                    .collapsible-panel.expanded .collapsible-content {
                        padding: 10px;
                        max-height: 10000px; /* Large enough to contain content */
                    }
                    
                    /* Disable hover effects during content updates */
                    body.updating-content * {
                        pointer-events: none !important;
                    }
                    
                    /* Keyboard focus styles */
                    .collapsible-header:focus {
                        outline: 2px solid #4d90fe;
                        outline-offset: -2px;
                    }
                </style>
                <script>
                    // Panel state tracking
                    let panelStates = {};
                    let isUpdatingContent = false;
                    let wasAtBottom = false;
                    
                    // Define the original function first
                    function updateContent(html) {
                        isUpdatingContent = true;
                        wasAtBottom = isScrolledToBottom();
                        
                        // Add a class to the body during updates to disable hover effects
                        document.body.classList.add('updating-content');
                        
                        // Save current scroll position and viewport height before update
                        const scrollPosition = window.scrollY;
                        const viewportHeight = window.innerHeight;
                        const documentHeight = document.body.scrollHeight;
                        const scrollPercentage = documentHeight > 0 ? scrollPosition / documentHeight : 0;
                        
                        // Store current panel states before updating
                        storeCurrentPanelStates();
                        
                        // Update content
                        document.getElementById('content').innerHTML = html;
                        
                        // Restore panel states
                        restorePanelStates();
                        
                        // Use requestAnimationFrame to ensure DOM is updated before scrolling
                        requestAnimationFrame(() => {
                            if (wasAtBottom) {
                                // If user was at bottom, scroll to new bottom
                                window.scrollTo(0, document.body.scrollHeight);
                            } else {
                                // Try to maintain user's scroll position
                                // First try absolute position
                                window.scrollTo(0, scrollPosition);
                                
                                // If content size changed significantly, try percentage-based position
                                setTimeout(() => {
                                    const newDocumentHeight = document.body.scrollHeight;
                                    // Only apply percentage-based scrolling for significant content changes
                                    if (Math.abs(newDocumentHeight - documentHeight) > viewportHeight * 0.3) {
                                        window.scrollTo(0, newDocumentHeight * scrollPercentage);
                                    }
                                }, 50);
                            }
                            
                            // Remove updating class after a short delay
                            setTimeout(() => {
                                document.body.classList.remove('updating-content');
                                isUpdatingContent = false;
                            }, 150);
                        });
                    }
                    
                    function isScrolledToBottom() {
                        // More generous threshold (100px) to determine if we're at the bottom
                        return (window.innerHeight + window.scrollY) >= (document.body.offsetHeight - 100);
                    }
                    
                    function getPanelId(panel) {
                        // Use data attribute if available (more stable)
                        if (panel.dataset.panelId) {
                            return panel.dataset.panelId;
                        }
                        
                        // Otherwise generate from header content (more stable than using innerHTML)
                        const header = panel.querySelector('.collapsible-header');
                        let title = '';
                        if (header) {
                            const titleElement = header.querySelector('.collapsible-title');
                            if (titleElement) {
                                title = titleElement.textContent || '';
                            }
                        }
                        
                        // Generate a more stable ID based on title and position in document
                        const allPanels = Array.from(document.querySelectorAll('.collapsible-panel'));
                        const panelIndex = allPanels.indexOf(panel);
                        const id = 'panel-' + title.toLowerCase().replace(/[^a-z0-9]/g, '-') + '-' + panelIndex;
                        
                        // Store ID as data attribute for future lookups
                        panel.dataset.panelId = id;
                        return id;
                    }
                    
                    function storeCurrentPanelStates() {
                        document.querySelectorAll('.collapsible-panel').forEach(panel => {
                            const panelId = getPanelId(panel);
                            panelStates[panelId] = panel.classList.contains('expanded');
                        });
                    }
                    
                    function restorePanelStates() {
                        document.querySelectorAll('.collapsible-panel').forEach(panel => {
                            const panelId = getPanelId(panel);
                            
                            // Apply stored state if it exists
                            if (panelStates.hasOwnProperty(panelId)) {
                                if (panelStates[panelId]) {
                                    panel.classList.add('expanded');
                                    const arrow = panel.querySelector('.collapsible-arrow');
                                    if (arrow) {
                                        arrow.textContent = '▼';
                                    }
                                } else {
                                    panel.classList.remove('expanded');
                                    const arrow = panel.querySelector('.collapsible-arrow');
                                    if (arrow) {
                                        arrow.textContent = '▶';
                                    }
                                }
                            }
                        });
                    }
                    
                    function scrollToBottom() {
                        window.scrollTo({
                            top: document.body.scrollHeight,
                            behavior: 'auto'
                        });
                    }
                    
                    // Initialize when DOM is ready
                    document.addEventListener('DOMContentLoaded', function() {
                        // Set up collapsible panels
                        document.addEventListener('click', function(e) {
                            if (isUpdatingContent) return;
                            
                            // Find closest collapsible header
                            const header = e.target.closest('.collapsible-header');
                            if (!header) return;
                            
                            const panel = header.closest('.collapsible-panel');
                            if (!panel) return;
                            
                            // Toggle expanded state
                            panel.classList.toggle('expanded');
                            
                            // Update arrow
                            const arrow = header.querySelector('.collapsible-arrow');
                            if (arrow) {
                                arrow.textContent = panel.classList.contains('expanded') ? '▼' : '▶';
                            }
                            
                            // Store panel state
                            const panelId = getPanelId(panel);
                            panelStates[panelId] = panel.classList.contains('expanded');
                        });
                        
                        // Add keyboard support for collapsible panels
                        document.addEventListener('keydown', function(e) {
                            if (isUpdatingContent) return;
                            
                            // Only handle Enter or Space
                            if (e.key !== 'Enter' && e.key !== ' ') return;
                            
                            const activeElement = document.activeElement;
                            if (activeElement && activeElement.classList.contains('collapsible-header')) {
                                e.preventDefault();
                                activeElement.click();
                            }
                        });
                        
                        // Make headers focusable
                        document.querySelectorAll('.collapsible-header').forEach(header => {
                            if (!header.hasAttribute('tabindex')) {
                                header.setAttribute('tabindex', '0');
                            }
                        });
                    });
                </script>
            </head>
            <body>
                <div id="content"></div>
            </body>
            </html>
        """.trimIndent()

        // Load the HTML template
        val dataUrl = "data:text/html;charset=utf-8;base64," + 
                      Base64.getEncoder().encodeToString(htmlTemplate.toByteArray(StandardCharsets.UTF_8))
        browser.loadURL(dataUrl)
    }

    override fun setMarkdown(markdown: String) {
        if (isDisposed.get()) return
        
        if (!loadCompleted.get()) {
            // Browser not ready yet, store content for later
            pendingContent.set(markdown)
            // Store content for processing (only after we've saved the pending content)
            currentContent = markdown
            return
        }
        
        // Process markdown in background thread
        AppExecutorUtil.getAppExecutorService().submit {
            try {
                if (isDisposed.get()) return@submit
                
                // Store content for processing
                currentContent = markdown
                
                // Process markdown to HTML
                val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)
                
                // Update UI on EDT
                SwingUtilities.invokeLater {
                    if (!isDisposed.get()) {
                        updateContentInBrowser(html)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error processing markdown", e)
            }
        }
    }
    
    private fun updateContentInBrowser(html: String) {
        if (isDisposed.get()) return
        
        try {
            // Execute JavaScript to update content
            val script = """
                try {
                    if (typeof updateContent === 'function') {
                        updateContent(${jsonEscapeString(html)});
                    } else {
                        // Basic fallback
                        document.getElementById('content').innerHTML = ${jsonEscapeString(html)};
                    }
                } catch (e) {
                    console.error("Error updating content:", e);
                    // Ultra basic fallback
                    document.getElementById('content').innerHTML = ${jsonEscapeString(html)};
                }
            """.trimIndent()
            
            executeJavaScript(script)
        } catch (e: Exception) {
            LOG.error("Error updating content in browser", e)
        }
    }

    override fun setDarkTheme(isDarkTheme: Boolean) {
        if (isDisposed.get()) return
        
        if (themeManager.updateTheme(isDarkTheme)) {
            // Apply theme change via CSS variables instead of full rerender
            executeJavaScript("""
                (function() {
                    // Update theme colors
                    document.body.style.backgroundColor = '${if (isDarkTheme) "#2b2b2b" else "#ffffff"}';
                    document.body.style.color = '${if (isDarkTheme) "#ffffff" else "#000000"}';
                    
                    // Add/remove theme class
                    if (${isDarkTheme}) {
                        document.body.classList.add('dark-theme');
                        document.body.classList.remove('light-theme');
                    } else {
                        document.body.classList.add('light-theme');
                        document.body.classList.remove('dark-theme');
                    }
                    
                    // Update code blocks and other themed elements without full re-render
                    document.querySelectorAll('.collapsible-panel').forEach(panel => {
                        panel.style.borderColor = '${if (isDarkTheme) "rgba(200, 200, 200, 0.2)" else "rgba(0, 0, 0, 0.2)"}';
                    });
                    
                    document.querySelectorAll('.collapsible-header').forEach(header => {
                        header.style.backgroundColor = '${if (isDarkTheme) "rgba(200, 200, 200, 0.1)" else "rgba(0, 0, 0, 0.1)"}';
                    });
                })();
            """.trimIndent())
        }
    }
    
    override fun scrollToBottom() {
        if (isDisposed.get() || !loadCompleted.get()) return
        
        try {
            // First immediate scroll attempt
            executeJavaScript("scrollToBottom();")
            
            // Single delayed scroll attempt as backup
            // This helps with cases where content is still being rendered
            if (!isDisposed.get()) {
                AppExecutorUtil.getAppScheduledExecutorService().schedule({
                    if (!isDisposed.get()) {
                        executeJavaScript("scrollToBottom();")
                    }
                }, 300, TimeUnit.MILLISECONDS)
            }
        } catch (e: Exception) {
            LOG.error("Error scrolling to bottom", e)
        }
    }
    
    override fun supportsDevTools(): Boolean = true
    
    override fun showDevTools(): Boolean {
        if (isDisposed.get()) return false
        
        try {
            val devTools = browser.openDevtools()
            if (devTools != null) {
                synchronized(devToolsInstances) {
                    devToolsInstances.add(devTools)
                }
                
                // Register a listener to remove from our tracking set when closed
                try {
                    val devToolsBrowser = devTools.devTools
                    if (devToolsBrowser != null) {
                        browser.jbCefClient.addLifeSpanHandler(object : org.cef.handler.CefLifeSpanHandlerAdapter() {
                            override fun onBeforeClose(browser: CefBrowser?) {
                                if (browser == devToolsBrowser) {
                                    synchronized(devToolsInstances) {
                                        devToolsInstances.remove(devTools)
                                    }
                                    browser.jbCefClient.removeLifeSpanHandler(browser)
                                }
                            }
                        }, devToolsBrowser)
                    }
                } catch (e: Exception) {
                    LOG.warn("Error setting up DevTools lifecycle tracking", e)
                }
            }
            return true
        } catch (e: Exception) {
            LOG.warn("Error opening DevTools", e)
            return false
        }
    }
    
    private fun executeJavaScript(script: String) {
        if (isDisposed.get()) return
        
        try {
            // Check if we're already on EDT
            if (SwingUtilities.isEventDispatchThread()) {
                if (!isDisposed.get()) {
                    browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
                }
            } else {
                // Only use invokeLater if we're not already on EDT
                SwingUtilities.invokeLater {
                    if (!isDisposed.get()) {
                        browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error executing JavaScript", e)
        }
    }
    
    /**
     * Properly escapes a string for use in JavaScript by converting it to a JSON string
     */
    private fun jsonEscapeString(str: String): String {
        // Create a StringBuilder with estimated capacity
        val sb = StringBuilder(str.length + 10)
        sb.append('"')
        
        // Manually escape all special characters
        for (c in str) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f") // Form feed
                '<' -> {
                    // Special handling for </script> to prevent XSS
                    if (str.indexOf("</script>", sb.length - 1) == sb.length - 1) {
                        sb.append("<\\/")
                    } else {
                        sb.append(c)
                    }
                }
                in '\u0000'..'\u001F' -> {
                    // Control characters need unicode escape
                    sb.append("\\u")
                    sb.append(String.format("%04x", c.code))
                }
                else -> sb.append(c)
            }
        }
        
        sb.append('"')
        return sb.toString()
    }
    
    override fun dispose() {
        // Only dispose once
        if (isDisposed.getAndSet(true)) return
        
        try {
            // Cancel any pending initialization
            initializationFuture?.cancel(true)
            
            // Ensure we're on EDT for Swing operations
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait { disposeOnEDT() }
                } catch (e: Exception) {
                    LOG.error("Error during invokeAndWait in dispose", e)
                    // Try to dispose anyway as a fallback
                    disposeOnEDT()
                }
            } else {
                disposeOnEDT()
            }
        } catch (e: Exception) {
            LOG.error("Error disposing JcefMarkdownRenderer", e)
        }
    }
    
    private fun disposeOnEDT() {
        try {
            // Close all DevTools windows
            synchronized(devToolsInstances) {
                devToolsInstances.forEach { devTools ->
                    try {
                        devTools.devTools?.close(true)
                    } catch (e: Exception) {
                        // Ignore errors when closing DevTools
                    }
                }
                devToolsInstances.clear()
            }
            
            // Dispose the parent disposable which will clean up all registered resources
            // This will also clean up the theme change listener and load handler
            Disposer.dispose(parentDisposable)
            
            // Clear panel
            mainPanel.removeAll()
        } catch (e: Exception) {
            LOG.error("Error in disposeOnEDT", e)
        }
    }
}
