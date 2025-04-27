package de.andrena.codingaider.outputview

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import org.cef.handler.CefLoadHandler
import org.cef.browser.CefBrowser
import org.cef.network.CefRequest
import org.cef.browser.CefFrame
import java.awt.Dimension
import com.intellij.ui.JBColor
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.data.MutableDataSet
import de.andrena.codingaider.utils.FilePathConverter
import java.util.Locale
import javax.swing.SwingUtilities
import java.util.ResourceBundle
import java.util.Timer
import java.util.TimerTask

class MarkdownJcefViewer(private val lookupPaths: List<String> = emptyList()) : MarkdownViewer {
    private val logger = Logger.getInstance(MarkdownJcefViewer::class.java)
    
    private val mainPanel = JPanel(BorderLayout()).apply {
        border = null
        minimumSize = Dimension(200, 100)
        preferredSize = Dimension(600, 400)
        isOpaque = true
        background = if (!JBColor.isBright()) JBColor(0x2B2B2B, 0x2B2B2B) else JBColor.WHITE
    }
    
    // Browser component
    private var jbCefBrowser: JBCefBrowser? = null
    
    // State tracking
    private var isDarkTheme = com.intellij.openapi.editor.colors.EditorColorsManager.getInstance().isDarkEditor
    private var currentContent = ""
    private val resourceBundle = ResourceBundle.getBundle("messages.MarkdownViewerBundle", Locale.getDefault())
    private var jcefLoadAttempts = 0
    private val maxJcefLoadAttempts = 5  // Maximum number of retry attempts
    
    // Scroll position tracking
    private var isUserScrolled = false
    private var lastScrollPosition = 0.0
    private var lastScrollRatio = 0.0
    private var lastContentHeight = 0

    init {
        // Remove any border from the main panel
        mainPanel.border = null
        initializeViewer()
    }

    private fun initializeViewer() {
        try {
            if (JBCefApp.isSupported()) {
                try {
                    initJcefBrowser()
                } catch (e: Exception) {
                    logger.warn("Error initializing JCEF browser: ${e.message}", e)
                    showErrorInBrowser("Failed to initialize browser component. Please restart IDE or check logs.")
                }
            } else {
                logger.warn("JCEF is not supported in this environment")
                showErrorInBrowser("Browser component is not supported in this environment.")
            }
        } catch (e: Exception) {
            logger.warn("Error initializing markdown viewer: ${e.message}", e)
            showErrorInBrowser("Failed to initialize markdown viewer. Please restart IDE or check logs.")
        }
    }
    
    private fun showErrorInBrowser(errorMessage: String) {
        try {
            // Create a simple browser with error message
            val browser = JBCefBrowser()
            jbCefBrowser = browser
            
            // Get error HTML template from resource bundle
            val errorHtmlTemplate = resourceBundle.getString("markdown.viewer.error.html")
            
            // Format the template with theme-specific colors
            val errorHtml = errorHtmlTemplate
                .replace("{0}", if (isDarkTheme) "#2b2b2b" else "#ffffff")
                .replace("{1}", if (isDarkTheme) "#ffffff" else "#000000")
                .replace("{2}", if (isDarkTheme) "#3c3f41" else "#f5f5f5")
                .replace("{3}", if (isDarkTheme) "#555" else "#ddd")
                .replace("{4}", if (isDarkTheme) "#f44336" else "#d32f2f")
                .replace("{5}", errorMessage)
            
            browser.loadHTML(errorHtml)
            mainPanel.add(browser.component, BorderLayout.CENTER)
            
        } catch (e: Exception) {
            logger.error("Failed to show error message: ${e.message}", e)
        }
    }
    
    private fun initJcefBrowser() {
        try {
            // Create browser with simple load handler
            val browser = JBCefBrowser()
            jbCefBrowser = browser
            
            browser.component.apply {
                isFocusable = true
                minimumSize = Dimension(200, 100)
                background = mainPanel.background
                // Enable native browser scrolling
                putClientProperty("JBCefBrowser.wrapperPanel.scrollable", true)
                putClientProperty("JBCefBrowser.useNativeScrollbar", true)
                // Allow JBScrollPane wrapping if needed
                putClientProperty("JBCefBrowser.parentScrollPane", true)
            }
            
            // Add scroll listener to track user scrolling
            browser.cefBrowser?.let { cefBrowser ->
                try {
                    // Use JavaScript to track scroll position
                    browser.jbCefClient?.addLoadHandler(object : CefLoadHandler {
                        override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                            if (frame != null && frame.parent == null) {
                                // Add scroll event listener after page loads with improved metrics
                                cefBrowser.executeJavaScript("""
                                    window.addEventListener('scroll', function() {
                                        // Track basic scroll metrics
                                        window.scrollPositionY = window.scrollY;
                                        window.scrollHeight = document.body.scrollHeight;
                                        window.viewportHeight = window.innerHeight;
                                        window.isUserScrolled = true;
                                        
                                        // Calculate and store scroll ratio (relative position)
                                        const maxScroll = Math.max(0, document.body.scrollHeight - window.innerHeight);
                                        window.lastScrollRatio = maxScroll > 0 ? window.scrollY / maxScroll : 0;
                                        
                                        // Track if we're at the bottom (within 20px)
                                        window.isAtBottom = (document.body.scrollHeight - window.scrollY - window.innerHeight) < 20;
                                    }, { passive: true });
                                    
                                    // Also track resize events as they affect scroll position
                                    window.addEventListener('resize', function() {
                                        window.viewportHeight = window.innerHeight;
                                        // Recalculate "at bottom" state after resize
                                        window.isAtBottom = (document.body.scrollHeight - window.scrollY - window.innerHeight) < 20;
                                    }, { passive: true });
                                """.trimIndent(), cefBrowser.url, 0)
                            }
                        }
                        override fun onLoadStart(browser: CefBrowser?, frame: CefFrame?, transitionType: CefRequest.TransitionType?) {}
                        override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {}
                        override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {}
                    }, cefBrowser)
                } catch (e: Exception) {
                    logger.warn("Could not add scroll tracking: ${e.message}")
                }
            }

            // Create the initial HTML with empty content
            val initialHtml = createHtmlWithContent("")
            
            // Load the initial HTML template directly
            browser.loadHTML(initialHtml)

            // Set a simple load handler
            try {
                val client: JBCefClient? = browser.jbCefClient
                if (client == null) {
                    logger.warn("JBCefClient is null")
                    showErrorInBrowser("Browser component initialization failed.")
                    return
                }
                
                val cefBrowser = browser.cefBrowser
                if (cefBrowser == null) {
                    logger.warn("CefBrowser is null")
                    showErrorInBrowser("Browser component initialization failed.")
                    return
                }
                
                client.addLoadHandler(object : CefLoadHandler {
                    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                        // Only act on main frame (main frame has no parent)
                        if (frame != null && frame.parent == null) {
                            // Reset the load attempts counter on successful load
                            jcefLoadAttempts = 0
                        }
                    }

                    override fun onLoadStart(browser: CefBrowser?, frame: CefFrame?, transitionType: CefRequest.TransitionType?) {}
                    override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {
                        // Only handle main frame errors
                        if (frame == null || frame.parent != null) {
                            return
                        }
                        
                        // Ignore benign ERR_ABORTED errors that happen during normal navigation
                        // when we call loadHTML again and abort the previous load
                        if (errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED) {
                            return
                        }
                        
                        // Log all other errors
                        logger.warn("JCEF load error: $errorCode - $errorText for URL: $failedUrl")
                        
                        // Handle common transient errors
                        if (errorCode == CefLoadHandler.ErrorCode.ERR_FAILED && 
                            jcefLoadAttempts < maxJcefLoadAttempts) {
                            
                            jcefLoadAttempts++
                            logger.info("Retrying JCEF load, attempt $jcefLoadAttempts of $maxJcefLoadAttempts for error: $errorCode")
                            
                            // Use capped exponential backoff for retries
                            val retryDelay = 200L * minOf(1L shl (jcefLoadAttempts - 1), 8L)
                            
                            // Schedule a retry after a delay with exponential backoff
                            Timer().schedule(object : TimerTask() {
                                override fun run() {
                                    SwingUtilities.invokeLater {
                                        try {
                                            // Try reloading the content with a fresh HTML template
                                            val html = convertMarkdownToHtml(currentContent)
                                            val fullHtml = createHtmlWithContent(html)
                                            jbCefBrowser?.loadHTML(fullHtml)
                                        } catch (e: Exception) {
                                            logger.warn("Error during JCEF retry: ${e.message}", e)
                                            if (jcefLoadAttempts >= maxJcefLoadAttempts) {
                                                showErrorInBrowser("Browser component failed to load content after multiple attempts.")
                                            }
                                        }
                                    }
                                }
                            }, retryDelay)
                            
                            return
                        }
                        
                        // For persistent errors, switch to fallback editor
                        logger.warn("Persistent JCEF error: $errorCode - $errorText")
                        showErrorInBrowser("Browser component failed to load content after multiple attempts.")
                    }
                    override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {}
                }, cefBrowser) // Pass the browser instance to handle only the main frame
            } catch (e: Exception) {
                logger.error("Exception during JCEF browser initialization: ${e.message}", e)
                showErrorInBrowser("Browser component initialization failed.")
            }
        } catch (e: Exception) {
            logger.error("Exception during JCEF browser creation: ${e.message}", e)
            showErrorInBrowser("Browser component initialization failed.")
        }

        mainPanel.add(jbCefBrowser!!.component, BorderLayout.CENTER)
    }

    /**
     * Creates the base HTML template with proper localization support
     */
    private fun createBaseHtml(
        backgroundColor: String,
        fontColor: String,
        scrollBarColor: String,
        scrollbarThumbColor: String,
        scrollbarHoverColor: String,
        preBackgroundColor: String
    ): String {
        val template = resourceBundle.getString("markdown.viewer.html.template")
        // Replace placeholders directly to avoid MessageFormat parsing issues with CSS/JS braces
        return template
            .replace("{0}", backgroundColor)
            .replace("{1}", fontColor)
            .replace("{2}", scrollBarColor)
            .replace("{3}", scrollbarThumbColor)
            .replace("{4}", scrollbarHoverColor)
            .replace("{5}", preBackgroundColor)
    }

    val component: JComponent
        get() = mainPanel

    /**
     * Sets the markdown content to be displayed
     */
    fun setMarkdown(markdown: String) {
        if (markdown.isBlank()) return
        
        // Only update if content has changed
        if (markdown == currentContent) {
            return
        }
        
        // Save current scroll position before updating content
        saveScrollPosition()
        
        currentContent = markdown
        
        val html = convertMarkdownToHtml(markdown)
        val full = createHtmlWithContent(html)

        jbCefBrowser?.let { browser ->
            // Use a more reliable approach to maintain scroll position
            // First check if we need to capture the current position
            captureScrollMetrics(browser)
            
            // Then load the new content
            browser.loadHTML(full)
            
            // Restore scroll position after content loads
            restoreScrollPositionAfterLoad(browser)
        }
    }
    
    /**
     * Captures detailed scroll metrics before content update
     */
    private fun captureScrollMetrics(browser: JBCefBrowser) {
        try {
            browser.cefBrowser?.executeJavaScript("""
                (function() {
                    // Get current scroll metrics
                    const scrollY = window.scrollY || document.documentElement.scrollTop || 0;
                    const scrollHeight = document.body.scrollHeight || document.documentElement.scrollHeight || 1;
                    const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 1;
                    
                    // Calculate scroll ratio (position relative to scrollable area)
                    const maxScroll = Math.max(0, scrollHeight - viewportHeight);
                    const scrollRatio = maxScroll > 0 ? scrollY / maxScroll : 0;
                    
                    // Store values in global variables for Java to access
                    window.lastScrollPosition = scrollY;
                    window.lastScrollRatio = scrollRatio;
                    window.lastContentHeight = scrollHeight;
                    window.lastViewportHeight = viewportHeight;
                    window.isUserScrolled = window.isUserScrolled === true || scrollY > 0;
                })();
            """.trimIndent(), browser.cefBrowser?.url ?: "", 0)
        } catch (e: Exception) {
            logger.warn("Error capturing scroll metrics: ${e.message}")
        }
    }
    
    /**
     * Saves the current scroll position
     */
    private fun saveScrollPosition() {
        try {
            jbCefBrowser?.let { browser ->
                browser.cefBrowser?.executeJavaScript("""
                    (function() {
                        // Get current scroll metrics
                        const scrollY = window.scrollY || document.documentElement.scrollTop || 0;
                        const scrollHeight = document.body.scrollHeight || document.documentElement.scrollHeight || 1;
                        const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 1;
                        
                        // Calculate scroll ratio (position relative to scrollable area)
                        const maxScroll = Math.max(0, scrollHeight - viewportHeight);
                        const scrollRatio = maxScroll > 0 ? scrollY / maxScroll : 0;
                        
                        // Store values in global variables for Java to access
                        window.javaScriptScrollPosition = {
                            y: scrollY,
                            height: scrollHeight,
                            viewportHeight: viewportHeight,
                            ratio: scrollRatio,
                            isUserScrolled: window.isUserScrolled === true || scrollY > 0,
                            isAtBottom: (scrollHeight - scrollY - viewportHeight) < 20
                        };
                        
                        // Also store in separate variables for redundancy
                        window.lastScrollPosition = scrollY;
                        window.lastScrollRatio = scrollRatio;
                        window.lastContentHeight = scrollHeight;
                        window.lastViewportHeight = viewportHeight;
                    })();
                """.trimIndent(), browser.cefBrowser?.url ?: "", 0)
            }
        } catch (e: Exception) {
            logger.warn("Error saving scroll position: ${e.message}")
        }
    }
    
    /**
     * Restores the scroll position after content loads
     */
    private fun restoreScrollPositionAfterLoad(browser: JBCefBrowser) {
        try {
            browser.cefBrowser?.let { cefBrowser ->
                // Add a load handler to restore position after content loads
                browser.jbCefClient?.addLoadHandler(object : CefLoadHandler {
                    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                        if (frame != null && frame.parent == null) {
                            // Restore scroll position using JavaScript with multiple strategies
                            cefBrowser.executeJavaScript("""
                                (function() {
                                    // Use a series of timeouts with increasing delays for more reliable restoration
                                    // as sometimes the DOM needs more time to fully render
                                    const attemptRestore = (attempt) => {
                                        // Get stored position data (try both storage methods)
                                        const scrollData = window.javaScriptScrollPosition || {};
                                        
                                        // Determine if we should restore position
                                        const shouldRestore = scrollData.isUserScrolled || 
                                                             window.isUserScrolled === true || 
                                                             window.lastScrollPosition > 0;
                                        
                                        if (shouldRestore) {
                                            // Get current document metrics
                                            const scrollHeight = document.body.scrollHeight || document.documentElement.scrollHeight || 1;
                                            const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 1;
                                            const maxScroll = Math.max(0, scrollHeight - viewportHeight);
                                            
                                            // Try multiple restoration strategies
                                            
                                            // Strategy 1: Use stored ratio (most reliable for content changes)
                                            const ratio = scrollData.ratio || window.lastScrollRatio || 0;
                                            const targetPos1 = Math.round(ratio * maxScroll);
                                            
                                            // Strategy 2: Try to maintain absolute position if content similar in size
                                            const oldPos = scrollData.y || window.lastScrollPosition || 0;
                                            const heightRatio = scrollHeight / (scrollData.height || window.lastContentHeight || 1);
                                            const targetPos2 = Math.round(oldPos * heightRatio);
                                            
                                            // Strategy 3: Check if user was at bottom and keep at bottom if so
                                            const wasAtBottom = scrollData.isAtBottom || 
                                                               ((scrollData.height - scrollData.y - scrollData.viewportHeight) < 20);
                                            
                                            // Choose best strategy
                                            let targetPos;
                                            if (wasAtBottom) {
                                                targetPos = maxScroll; // Stay at bottom
                                            } else {
                                                // Use ratio-based position as primary strategy
                                                targetPos = targetPos1;
                                                
                                                // If content size is similar, consider absolute position too
                                                const sizeChange = Math.abs(1 - (scrollHeight / (scrollData.height || 1)));
                                                if (sizeChange < 0.2) { // If size changed less than 20%
                                                    // Blend the two strategies
                                                    targetPos = Math.min(maxScroll, Math.max(0, 
                                                        Math.round(targetPos * 0.7 + targetPos2 * 0.3)));
                                                }
                                            }
                                            
                                            // Apply scroll position
                                            window.scrollTo(0, Math.min(maxScroll, Math.max(0, targetPos)));
                                            
                                            // Preserve user scroll state
                                            window.isUserScrolled = true;
                                            
                                            // If we're on the last attempt and still not close to target, try once more
                                            if (attempt === 2 && Math.abs(window.scrollY - targetPos) > 50) {
                                                setTimeout(() => window.scrollTo(0, targetPos), 50);
                                            }
                                        }
                                    };
                                    
                                    // Try restoration at increasing intervals
                                    setTimeout(() => attemptRestore(0), 10);
                                    setTimeout(() => attemptRestore(1), 50);
                                    setTimeout(() => attemptRestore(2), 200);
                                })();
                            """.trimIndent(), cefBrowser.url, 0)
                        }
                    }
                    override fun onLoadStart(browser: CefBrowser?, frame: CefFrame?, transitionType: CefRequest.TransitionType?) {}
                    override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {}
                    override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {}
                }, cefBrowser)
            }
        } catch (e: Exception) {
            logger.warn("Error setting up scroll position restoration: ${e.message}")
        }
    }

    private fun createHtmlWithContent(content: String): String {
        val baseHtml = createBaseHtml(
            if (this@MarkdownJcefViewer.isDarkTheme) "#2b2b2b" else "#ffffff",
            if (this@MarkdownJcefViewer.isDarkTheme) "#ffffff" else "#000000",
            if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f1f1f1",
            if (this@MarkdownJcefViewer.isDarkTheme) "#555" else "#c1c1c1",
            if (this@MarkdownJcefViewer.isDarkTheme) "#777" else "#a1a1a1",
            if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f5f5f5"
        )
        
        // Simple content replacement
        return baseHtml.replace("<div id=\"content\"></div>", "<div id=\"content\">$content</div>")
    }
    
    /**
     * Ensures the browser is properly initialized
     */
    fun ensureContentDisplayed() {
        SwingUtilities.invokeLater {
            if (currentContent.isNotEmpty()) {
                // If browser isn't ready yet but we have content, try to initialize
                if (jbCefBrowser == null) {
                    initializeViewer()
                }
                
                // If we've had too many failures, show error message
                if (jcefLoadAttempts >= maxJcefLoadAttempts) {
                    showErrorInBrowser("Browser component failed to load content after multiple attempts.")
                }
            }
        }
    }

    fun setDarkTheme(dark: Boolean) {
        // Only reload if theme actually changed
        if (isDarkTheme != dark) {
            isDarkTheme = dark
            if (currentContent.isNotEmpty()) {
                try {
                    // Reset load attempts when theme changes
                    jcefLoadAttempts = 0
                    
                    // Reload with new theme
                    jbCefBrowser?.loadHTML(createBaseHtml(
                        if (this@MarkdownJcefViewer.isDarkTheme) "#2b2b2b" else "#ffffff",
                        if (this@MarkdownJcefViewer.isDarkTheme) "#ffffff" else "#000000",
                        if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f1f1f1",
                        if (this@MarkdownJcefViewer.isDarkTheme) "#555" else "#c1c1c1",
                        if (this@MarkdownJcefViewer.isDarkTheme) "#777" else "#a1a1a1",
                        if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f5f5f5"
                    ))
                    
                    // Only update content if we need to
                    setMarkdown(currentContent)
                } catch (e: Exception) {
                    logger.warn("Error updating theme: ${e.message}", e)
                }
            }
        }
    }

    private val markdownOptions = MutableDataSet().apply {
        set(
            Parser.EXTENSIONS, listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create(),
                TaskListExtension.create(),
                DefinitionExtension.create(),
                FootnoteExtension.create(),
                TocExtension.create()
            )
        )
    }

    private fun convertMarkdownToHtml(markdown: String): String {
        val parser = Parser.builder(markdownOptions).build()
        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
        val basePath = project?.let { it.basePath }
        val processedMarkdown = FilePathConverter.convertPathsToMarkdownLinks(markdown, basePath)
        val document = parser.parse(processedMarkdown)
        val renderer = HtmlRenderer.builder(markdownOptions).build()
        
        // Render the basic markdown
        var html = renderer.render(document)
        
        // Process special aider blocks
        html = processAiderBlocks(html, parser, renderer)
        
        // Add styling and process search/replace blocks
        return applyStylesToHtml(html)
    }
    
    private fun processAiderBlocks(html: String, parser: Parser, renderer: HtmlRenderer): String {
        return html.replace(
            Regex("(?s)<aider-intention>\\s*(.*?)\\s*</aider-intention>")) { matchResult ->
                val intentionContent = matchResult.groupValues[1].trim()
                val renderedContent = renderer.render(parser.parse(intentionContent))
                "<div class=\"aider-intention\">$renderedContent</div>"
        }.replace(
            Regex("(?s)<aider-summary>\\s*(.*?)\\s*</aider-summary>")) { matchResult ->
                val summaryContent = matchResult.groupValues[1].trim()
                val renderedContent = renderer.render(parser.parse(summaryContent))
                "<div class=\"aider-summary\">$renderedContent</div>"
        }
    }
    
    private fun applyStylesToHtml(html: String): String {
        // Define colors based on theme
        val colors = if (isDarkTheme) {
            mapOf(
                "bodyBg" to "#2b2b2b",
                "bodyText" to "#ffffff",
                "preBg" to "#1e1e1e",
                "preBorder" to "#666666",
                "searchBg" to "#362a1e",
                "replaceBg" to "#1e3626",
                "searchText" to "#ff8c7c",
                "replaceText" to "#7cff8c",
                "intentionBg" to "#1a2733",
                "intentionBorder" to "#2c4356",
                "intentionText" to "#a8c7f0",
                "summaryBg" to "#2d2d2d",
                "summaryBorder" to "#454545",
                "summaryText" to "#e8e8e8"
            )
        } else {
            mapOf(
                "bodyBg" to "#ffffff", 
                "bodyText" to "#000000",
                "preBg" to "#f5f5f5",
                "preBorder" to "#cccccc",
                "searchBg" to "#ffedeb",
                "replaceBg" to "#ebffed", 
                "searchText" to "#d73a49",
                "replaceText" to "#28a745",
                "intentionBg" to "#f0f7ff",
                "intentionBorder" to "#bcd6f5",
                "intentionText" to "#0066cc",
                "summaryBg" to "#fafafa",
                "summaryBorder" to "#e5e5e5",
                "summaryText" to "#2b2b2b"
            )
        }

        // Get CSS styles from resource bundle and format with colors
        val cssTemplate = resourceBundle.getString("markdown.viewer.css.styles")

        // Replace placeholders directly to avoid MessageFormat parsing issues with CSS braces
        val formattedCss = cssTemplate
            .replace("{0}", colors["bodyBg"] ?: "")
            .replace("{1}", colors["bodyText"] ?: "")
            .replace("{2}", colors["preBg"] ?: "")
            .replace("{3}", colors["preBorder"] ?: "")
            .replace("{4}", colors["searchBg"] ?: "")
            .replace("{5}", colors["searchText"] ?: "")
            .replace("{6}", colors["replaceBg"] ?: "")
            .replace("{7}", colors["replaceText"] ?: "")
            .replace("{8}", if (isDarkTheme) "#555" else "#ddd")
            .replace("{9}", if (isDarkTheme) "#3c3f41" else "#f0f0f0")
            .replace("{10}", colors["intentionBg"] ?: "")
            .replace("{11}", colors["intentionBorder"] ?: "")
            .replace("{12}", colors["intentionText"] ?: "")
            .replace("{13}", colors["summaryBg"] ?: "")
            .replace("{14}", colors["summaryBorder"] ?: "")
            .replace("{15}", colors["summaryText"] ?: "")

        // Apply CSS styles
        val styledHtml = """
        <style>
            $formattedCss
        </style>
        
        <script>
            // This script is included for compatibility with the fallback editor
            // The main functionality is now in the base HTML template
        </script>
        
        ${processSearchReplaceBlocks(html)}
        """

        return styledHtml
    }

    private fun escapeHtml(text: String): String {
        // Use Apache Commons Text for HTML escaping (already included in IntelliJ)
        return try {
            org.apache.commons.text.StringEscapeUtils.escapeHtml4(text)
        } catch (e: NoClassDefFoundError) {
            // Fallback for 3rd-party IDEs that might not have Apache Commons Text
            text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }
    }

    /**
     * Processes search/replace blocks and other special formatting
     */
    private fun processSearchReplaceBlocks(html: String): String {
        var processedHtml = html

        // Helper function to create collapsible panels
        fun createCollapsiblePanel(title: String, content: String, cssClass: String = "", isEscaped: Boolean = true): String {
            val contentHtml = if (isEscaped) "<pre><code>${escapeHtml(content.trim())}</code></pre>" else content.trim()
            
            val template = resourceBundle.getString("markdown.viewer.collapsible.panel")
            return template
                .replace("{0}", cssClass)
                .replace("{1}", title)
                .replace("{2}", contentHtml)
        }

        // Process standard blocks
        val blockPatterns = mapOf(
            """<aider-command>\s*(.*?)\s*</aider-command>""" to 
                { content: String -> createCollapsiblePanel("Aider Command", content) },
            
            """<aider-system-prompt>(.*?)</aider-system-prompt>""" to 
                { content: String -> createCollapsiblePanel("System Prompt", content, "system") },
            
            """<aider-user-prompt>(.*?)</aider-user-prompt>""" to 
                { content: String -> createCollapsiblePanel("User Request", content, "user") },
            
            """<div class="aider-intention">(.*?)</div>""" to 
                { content: String -> createCollapsiblePanel("Intention", content, "intention", isEscaped = false) },
            
            """<div class="aider-summary">(.*?)</div>""" to 
                { content: String -> createCollapsiblePanel("Summary", content, "summary", isEscaped = false) }
        )

        // Apply block patterns
        blockPatterns.forEach { (pattern, formatter) ->
            processedHtml = processedHtml.replace(
                Regex(pattern, RegexOption.DOT_MATCHES_ALL)
            ) { matchResult ->
                formatter(matchResult.groupValues[1])
            }
        }

        // Process search/replace blocks - improved to better handle edit format blocks
        // Use possessive quantifiers to prevent catastrophic backtracking
        val searchReplacePattern = Regex("""(?m)^([^\n]++)\n```[^\n]*+\n<<<<<<< SEARCH\n(.*+)\n=======\n(.*+)\n>>>>>>> REPLACE\n```""", 
            RegexOption.DOT_MATCHES_ALL)
        
        processedHtml = processedHtml.replace(searchReplacePattern) { matchResult ->
            val filePath = matchResult.groupValues[1].trim()
            val searchBlock = matchResult.groupValues[2]
            val replaceBlock = matchResult.groupValues[3]
            
            """
            <div class="collapsible-panel expanded edit-format-panel">
                <div class="collapsible-header edit-format">
                    <span class="collapsible-title">${escapeHtml(filePath)}</span>
                    <span class="collapsible-arrow">▼</span>
                </div>
                <div class="collapsible-content">
                    <pre class="edit-format-content">
                        <code class="search-block">${escapeHtml(searchBlock.trim())}</code>
                        <code class="replace-block">${escapeHtml(replaceBlock.trim())}</code>
                    </pre>
                </div>
            </div>
            """.trimIndent()
        }

        return processedHtml
    }
}
