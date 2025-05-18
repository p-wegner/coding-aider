package de.andrena.codingaider.outputview.markdown

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.UIUtil
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.schedule

/**
 * Markdown renderer implementation using JCEF (Chromium Embedded Framework)
 * for high-quality HTML rendering with JavaScript support.
 */
class JcefMarkdownRenderer(
    private val contentProcessor: MarkdownContentProcessor,
    private val themeManager: MarkdownThemeManager
) : MarkdownRenderer {
    private var isDisposed = false
    private val browser: JBCefBrowser = JBCefBrowser()
    private val mainPanel = JPanel(BorderLayout())
    private val loadCompleted = AtomicBoolean(false)
    private val jsQuery = JBCefJSQuery.create(browser)
    private var currentContent = ""
    private var pendingContent: String? = null
    private var initializationTimer: Timer? = null
    private var devToolsOpened = false

    override val component: JComponent
        get() = mainPanel

    override val isReady: Boolean
        get() = loadCompleted.get() && !isDisposed

    init {
        // Configure browser and panel
        browser.component.minimumSize = Dimension(200, 100)
        browser.component.preferredSize = Dimension(600, 400)
        mainPanel.add(browser.component, BorderLayout.CENTER)
        
        // Set background color to match IDE theme
//        val bgColor = if (themeManager.isDarkTheme) "#2b2b2b" else "#ffffff"
//        browser.cefBrowser.backgroundColor = java.awt.Color.decode(bgColor)
        
        // Listen for theme changes
        themeManager.addThemeChangeListener { isDark ->
            if (!isDisposed) {
                setDarkTheme(isDark)
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
                if (!isLoading && !loadCompleted.getAndSet(true)) {
                    // Browser is ready, apply any pending content
                    SwingUtilities.invokeLater {
                        pendingContent?.let { content ->
                            updateContent(content)
                            pendingContent = null
                        }
                    }
                }
            }
        }, browser.cefBrowser)
        
        // Set up initialization timer as a fallback
        initializationTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (!loadCompleted.getAndSet(true)) {
                        SwingUtilities.invokeLater {
                            pendingContent?.let { content ->
                                updateContent(content)
                                pendingContent = null
                            }
                        }
                    }
                    initializationTimer?.cancel()
                    initializationTimer = null
                }
            }, 2000) // 2 second fallback
        }
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
                    }
                    #content {
                        padding: 20px;
                    }
                </style>
                <script>
                    // Panel state tracking
                    let panelStates = {};
                    let isScrolledToBottom = true;
                    let isUpdatingContent = false;
                    
                    // Store original update function for later override
                    let originalUpdateContent;
                    
                    function isScrolledToBottom() {
                        // More generous threshold (100px) to determine if we're at the bottom
                        return (window.innerHeight + window.scrollY) >= (document.body.offsetHeight - 100);
                    }
                    
                    function getPanelId(panel) {
                        const header = panel.querySelector('.collapsible-header');
                        let title = '';
                        if (header) {
                            const titleElement = header.querySelector('.collapsible-title');
                            if (titleElement) {
                                title = titleElement.textContent || '';
                            }
                        }
                        
                        return 'panel-' + title.toLowerCase().replace(/[^a-z0-9]/g, '-') + '-' + 
                               Math.abs(panel.innerHTML.split('').reduce((a, b) => (a * 31 + b.charCodeAt(0)) & 0xFFFFFFFF, 0));
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
                    
                    // Initialize when DOM is ready
                    document.addEventListener('DOMContentLoaded', function() {
                        // Set up collapsible panels
                        document.addEventListener('click', function(e) {
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
                        
                        
                        // Override updateContent function with our enhanced version
                        originalUpdateContent = updateContent;
                        window.updateContent = function(html) {
                            isUpdatingContent = true;
                        
                            // Add a class to the body during updates to disable hover effects
                            document.body.classList.add('updating-content');
                        
                            // Save current scroll position and viewport height before update
                            const scrollPosition = window.scrollY;
                            const viewportHeight = window.innerHeight;
                            const documentHeight = document.body.scrollHeight;
                            const scrollPercentage = documentHeight > 0 ? scrollPosition / documentHeight : 0;
                            
                            // Calculate visible content range
                            const visibleStartY = scrollPosition;
                            const visibleEndY = scrollPosition + viewportHeight;
                            
                            // Store current panel states before updating
                            storeCurrentPanelStates();
                        
                            // Update content
                            document.getElementById('content').innerHTML = html;
                        
                            // Restore panel states
                            restorePanelStates();
                        
                            // Use requestAnimationFrame to ensure DOM is updated before scrolling
                            requestAnimationFrame(() => {
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
                        };
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
        if (isDisposed) return
        
        currentContent = markdown
        
        if (!loadCompleted.get()) {
            // Browser not ready yet, store content for later
            pendingContent = markdown
            return
        }
        
        updateContent(markdown)
    }
    
    private fun updateContent(markdown: String) {
        if (isDisposed) return
        
        try {
            // Process markdown to HTML
            val html = contentProcessor.processMarkdown(markdown, themeManager.isDarkTheme)
            
            // Execute JavaScript to update content
            val script = """
                try {
                    if (typeof window.updateContent === 'function') {
                        window.updateContent(`${escapeJsString(html)}`);
                    } else {
                        // Fallback if our custom function isn't available
                        const scrollPosition = window.scrollY;
                        
                        // Store panel states before update if function exists
                        if (typeof storeCurrentPanelStates === 'function') {
                            storeCurrentPanelStates();
                        }
                        
                        document.getElementById('content').innerHTML = `${escapeJsString(html)}`;
                        
                        // Restore panel states after update if function exists
                        if (typeof restorePanelStates === 'function') {
                            restorePanelStates();
                        }
                        
                        // Try to maintain the user's scroll position
                        window.scrollTo(0, scrollPosition);
                    }
                } catch (e) {
                    console.error("Error updating content:", e);
                    // Basic fallback
                    document.getElementById('content').innerHTML = `${escapeJsString(html)}`;
                }
            """.trimIndent()
            
            executeJavaScript(script)
        } catch (e: Exception) {
            println("Error updating content in JcefMarkdownRenderer: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun setDarkTheme(isDarkTheme: Boolean) {
        if (isDisposed) return
        
        if (themeManager.updateTheme(isDarkTheme) && currentContent.isNotEmpty()) {
            // Theme changed, update content with new theme
            updateContent(currentContent)
        }
    }
    
    
    override fun scrollToBottom() {
        if (isDisposed || !loadCompleted.get()) return
        
        try {
            executeJavaScript("""
                // Mark that we're doing a programmatic scroll
                isUpdatingContent = true;
                
                // Use the smooth scroll function if available
                if (typeof scrollToBottom === 'function') {
                    scrollToBottom();
                } else {
                    window.scrollTo({
                        top: document.body.scrollHeight,
                        behavior: 'auto'
                    });
                }
                
                // Reset updating flag after scroll completes
                setTimeout(() => { isUpdatingContent = false; }, 100);
            """)
            
            // Use just two strategic scroll attempts with longer delays
            for (delay in listOf(200L, 600L)) {
                Timer().schedule(delay) {
                    if (!isDisposed) {
                        SwingUtilities.invokeLater {
                            executeJavaScript("""
                                isUpdatingContent = true;
                                window.scrollTo({
                                    top: document.body.scrollHeight,
                                    behavior: 'auto'
                                });
                                setTimeout(() => { isUpdatingContent = false; }, 50);
                            """)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error scrolling to bottom: ${e.message}")
        }
    }
    
    override fun supportsDevTools(): Boolean = true
    
    override fun showDevTools(): Boolean {
        if (isDisposed) return false
        
        try {
            browser.openDevtools()
            devToolsOpened = true
            return true
        } catch (e: Exception) {
            println("Error opening DevTools: ${e.message}")
            return false
        }
    }
    
    private fun executeJavaScript(script: String) {
        if (isDisposed) return
        
        try {
            ApplicationManager.getApplication().invokeLater {
                if (!isDisposed) {
                    browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
                }
            }
        } catch (e: Exception) {
            println("Error executing JavaScript: ${e.message}")
        }
    }
    
    private fun escapeJsString(str: String): String {
        return str.replace("\\", "\\\\")
                 .replace("`", "\\`")
                 .replace("$", "\\$")
    }
    
    override fun dispose() {
        if (!isDisposed) {
            isDisposed = true
            try {
                initializationTimer?.cancel()
                initializationTimer = null
                
                // Clean up browser resources
                jsQuery.dispose()
                
                if (devToolsOpened) {
                    try {
                        browser.cefBrowser.devTools.close(true)
                    } catch (e: Exception) {
                        // Ignore errors when closing DevTools
                    }
                }
                
                browser.dispose()
                mainPanel.removeAll()
            } catch (e: Exception) {
                println("Error disposing JcefMarkdownRenderer: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
