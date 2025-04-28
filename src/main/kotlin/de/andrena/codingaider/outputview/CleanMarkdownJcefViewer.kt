package de.andrena.codingaider.outputview

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import de.andrena.codingaider.utils.FilePathConverter
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.Locale
import java.util.ResourceBundle
import java.util.Timer
import java.util.TimerTask
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * A clean implementation of MarkdownViewer using JCEF (JetBrains Chromium Embedded Framework).
 * This implementation focuses on:
 * - Clean, maintainable code
 * - Proper scroll position preservation
 * - Efficient content updates
 * - Collapsible panels for content structure
 * - Correct formatting of search/replace blocks
 * - Theme-aware rendering
 */
class CleanMarkdownJcefViewer(private val lookupPaths: List<String> = emptyList()) : MarkdownViewer {
    private val logger = Logger.getInstance(CleanMarkdownJcefViewer::class.java)
    private val resourceBundle = ResourceBundle.getBundle("messages.MarkdownViewerBundle", Locale.getDefault())
    
    // Main UI components
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
    private var isInitialized = false
    private var pendingMarkdown: String? = null
    private var isDarkTheme = EditorColorsManager.getInstance().schemeForCurrentUITheme.isDark
    private var currentContent = ""
    private var loadAttempts = 0
    private val maxLoadAttempts = 3
    
    // JavaScript bridge for communication with the browser
    private var jsQuery: JBCefJSQuery? = null
    
    // Flexmark for markdown parsing
    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create()
        ))
    }
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()
    
    // Search/Replace block pattern
    // Match both triple and quadruple backtick formats for search/replace blocks
    private val searchReplacePattern = Pattern.compile(
        """(?m)^([^\r\n]+)\r?\n```{1,4}(?:[^\r\n]*)\r?\n<<<<<<< SEARCH\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n```{1,4}$""",
        Pattern.DOTALL
    )
    
    init {
        initializeViewer()
    }
    
    private fun initializeViewer() {
        try {
            if (JBCefApp.isSupported()) {
                initJcefBrowser()
            } else {
                logger.warn("JCEF is not supported in this environment")
                showErrorMessage("Browser component is not supported in this environment.")
            }
        } catch (e: Exception) {
            logger.error("Error initializing markdown viewer", e)
            showErrorMessage("Failed to initialize markdown viewer: ${e.message}")
        }
    }
    
    private fun initJcefBrowser() {
        try {
            val browser = JBCefBrowser()
            jbCefBrowser = browser
            
            browser.component.apply {
                isFocusable = true
                minimumSize = Dimension(200, 100)
                background = mainPanel.background
            }
            
            // Create JS bridge for file path clicks
            jsQuery = JBCefJSQuery.create(browser).apply {
                addHandler { filePath ->
                    handleFilePathClick(filePath)
                    null
                }
            }
            
            // Add load handler
            browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
                override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                    if (frame != null && frame.parent == null) {
                        isInitialized = true
                        
                        // Connect JS bridge
                        connectJsBridge()
                        
                        // Process any pending markdown
                        pendingMarkdown?.let {
                            SwingUtilities.invokeLater {
                                setMarkdown(it)
                                pendingMarkdown = null
                            }
                        }
                    }
                }
                
                override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {
                    if (frame != null && frame.parent == null && errorCode != CefLoadHandler.ErrorCode.ERR_ABORTED) {
                        logger.warn("JCEF load error: $errorCode - $errorText for URL: $failedUrl")
                        
                        if (loadAttempts < maxLoadAttempts) {
                            loadAttempts++
                            Timer(true).schedule(object : TimerTask() {
                                override fun run() {
                                    SwingUtilities.invokeLater {
                                        loadHtmlTemplate()
                                    }
                                }
                            }, 500)
                        } else {
                            showErrorMessage("Failed to load browser component after multiple attempts.")
                        }
                    }
                }
            }, browser.cefBrowser)
            
            mainPanel.add(browser.component, BorderLayout.CENTER)
            
            // Load initial HTML template
            loadHtmlTemplate()
        } catch (e: Exception) {
            logger.error("Error initializing JCEF browser", e)
            showErrorMessage("Failed to initialize browser component: ${e.message}")
        }
    }
    
    private fun connectJsBridge() {
        jsQuery?.let { query ->
            jbCefBrowser?.cefBrowser?.executeJavaScript(
                "window.filePathClicked = function(path) { ${query.inject("path")} };",
                jbCefBrowser?.cefBrowser?.url ?: "",
                0
            )
        }
    }
    
    private fun handleFilePathClick(filePath: String) {
        // Open the file in the editor
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
            if (project != null) {
                val normalizedPath = filePath.trim()
                // This would be implemented to open the file in the editor
                logger.info("File path clicked: $normalizedPath")
            }
        } catch (e: Exception) {
            logger.error("Error handling file path click", e)
        }
    }
    
    override val component: JComponent
        get() = mainPanel
    
    override fun setMarkdown(markdown: String) {
        if (!isInitialized) {
            pendingMarkdown = markdown
            return
        }
        
        if (markdown == currentContent) {
            return
        }
        
        currentContent = markdown
        
        try {
            // Process the markdown content
            val processedHtml = convertMarkdownToHtml(markdown)
            
            // Update the browser content
            val script = """
                (function() {
                    const content = document.getElementById('content');
                    if (!content) return;
                    
                    // Store scroll position and panel states before update
                    const isAtBottom = (window.innerHeight + window.scrollY) >= document.body.offsetHeight - 50;
                    const scrollY = window.scrollY;
                    const userScrolled = window.isUserScrolled === true;
                    
                    // Store expanded panels
                    const expandedPanels = [];
                    document.querySelectorAll('.collapsible-panel.expanded').forEach(panel => {
                        const header = panel.querySelector('.collapsible-header');
                        if (header) {
                            const title = header.querySelector('.collapsible-title');
                            if (title) {
                                expandedPanels.push(title.textContent);
                            }
                        }
                    });
                    
                    // Update content
                    content.innerHTML = `${processedHtml}`;
                    
                    // Initialize collapsible panels
                    if (typeof initCollapsiblePanels === 'function') {
                        initCollapsiblePanels();
                        
                        // Restore expanded panels
                        expandedPanels.forEach(title => {
                            document.querySelectorAll('.collapsible-header').forEach(header => {
                                const headerTitle = header.querySelector('.collapsible-title');
                                if (headerTitle && headerTitle.textContent === title) {
                                    const panel = header.parentElement;
                                    if (panel && !panel.classList.contains('expanded')) {
                                        panel.classList.add('expanded');
                                        const arrow = header.querySelector('.collapsible-arrow');
                                        if (arrow) arrow.textContent = '▼';
                                    }
                                }
                            });
                        });
                    }
                    
                    // Restore scroll position
                    setTimeout(() => {
                        if (isAtBottom) {
                            window.scrollTo(0, document.body.scrollHeight);
                        } else if (!userScrolled) {
                            window.scrollTo(0, scrollY);
                        }
                    }, 10);
                })();
            """.trimIndent()
            
            jbCefBrowser?.cefBrowser?.executeJavaScript(script, jbCefBrowser?.cefBrowser?.url ?: "", 0)
        } catch (e: Exception) {
            logger.error("Error rendering markdown", e)
            showErrorMessage("Failed to render markdown: ${e.message}")
        }
    }
    
    override fun ensureContentDisplayed() {
        if (isInitialized && pendingMarkdown != null) {
            setMarkdown(pendingMarkdown!!)
            pendingMarkdown = null
        } else if (!isInitialized && jbCefBrowser == null) {
            // Try to initialize again
            initializeViewer()
        }
    }
    
    override fun setDarkTheme(dark: Boolean) {
        if (isDarkTheme != dark) {
            isDarkTheme = dark
            loadHtmlTemplate()
            pendingMarkdown = currentContent
        }
    }
    
    /**
     * Loads the HTML template with appropriate theme colors
     */
    private fun loadHtmlTemplate() {
        try {
            val templateKey = "markdown.viewer.html.template"
            val template = resourceBundle.getString(templateKey)
            
            // Define colors based on current theme
            val backgroundColor = if (isDarkTheme) "#2B2B2B" else "#FFFFFF"
            val textColor = if (isDarkTheme) "#CCCCCC" else "#000000"
            val scrollbarColor = if (isDarkTheme) "#1E1E1E" else "#F1F1F1"
            val scrollbarThumbColor = if (isDarkTheme) "#555555" else "#C1C1C1"
            val scrollbarHoverColor = if (isDarkTheme) "#777777" else "#A1A1A1"
            val codeBackgroundColor = if (isDarkTheme) "#1E1E1E" else "#F5F5F5"
            val linkColor = if (isDarkTheme) "#6A9BFF" else "#0366D6"
            val quoteColor = if (isDarkTheme) "#A0A0A0" else "#6A737D"
            
            // Format the template with theme-specific colors
            val html = template
                .replace("{0}", backgroundColor)
                .replace("{1}", textColor)
                .replace("{2}", scrollbarColor)
                .replace("{3}", scrollbarThumbColor)
                .replace("{4}", codeBackgroundColor)
                .replace("{5}", scrollbarHoverColor)
                .replace("{6}", linkColor)
                .replace("{7}", quoteColor)
            
            jbCefBrowser?.loadHTML(html)
        } catch (e: Exception) {
            logger.error("Error loading HTML template", e)
            showErrorMessage("Failed to load HTML template: ${e.message}")
        }
    }
    
    /**
     * Convert markdown to HTML with special formatting
     */
    private fun convertMarkdownToHtml(markdown: String): String {
        // Process file paths to make them clickable
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        val basePath = project?.basePath
        val processedMarkdown = FilePathConverter.convertPathsToMarkdownLinks(markdown, basePath)
        
        // Process search/replace blocks before Flexmark parsing
        val preprocessed = processSearchReplaceBlocks(processedMarkdown)
        
        // Convert to HTML using Flexmark
        val document = parser.parse(preprocessed)
        var html = renderer.render(document)
        
        // Escape JavaScript special characters
        html = html.replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("`", "\\`")
            .replace("$", "\\$") // Escape $ to prevent ${} template literal issues
        
        return html
    }
    
    /**
     * Process search/replace blocks to add special formatting
     * This is now called BEFORE Flexmark parsing, so we need to return markdown
     */
    private fun processSearchReplaceBlocks(markdown: String): String {
        val matcher = searchReplacePattern.matcher(markdown)
        val buffer = StringBuffer()
        
        while (matcher.find()) {
            val filePath = matcher.group(1)
            val searchContent = matcher.group(2)
            val replaceContent = matcher.group(3)
            
            // Create a markdown replacement that will render as a special panel
            val replacement = """
                <div class="edit-format-panel">
                <div class="file-path">$filePath</div>
                <div class="edit-format">SEARCH/REPLACE</div>
                <div class="edit-format-content">
                <div class="search-block">
                
                ```
                $searchContent
                ```
                
                </div>
                <div class="replace-block">
                
                ```
                $replaceContent
                ```
                
                </div>
                </div>
                </div>
            """.trimIndent()
            
            matcher.appendReplacement(buffer, replacement.replace("$", "\\$"))
        }
        matcher.appendTail(buffer)
        
        return buffer.toString()
    }
    
    /**
     * Show an error message in the browser
     */
    private fun showErrorMessage(message: String) {
        try {
            val errorTemplate = resourceBundle.getString("markdown.viewer.error.html")
            
            // Define colors based on current theme
            val backgroundColor = if (isDarkTheme) "#2B2B2B" else "#FFFFFF"
            val textColor = if (isDarkTheme) "#CCCCCC" else "#000000"
            val errorBackgroundColor = if (isDarkTheme) "#3C3F41" else "#F5F5F5"
            val errorBorderColor = if (isDarkTheme) "#5A5A5A" else "#DDDDDD"
            val errorTitleColor = if (isDarkTheme) "#FF6B68" else "#DB5860"
            
            // Format the template with theme-specific colors and error message
            val html = errorTemplate
                .replace("{0}", backgroundColor)
                .replace("{1}", textColor)
                .replace("{2}", errorBackgroundColor)
                .replace("{3}", errorBorderColor)
                .replace("{4}", errorTitleColor)
                .replace("{5}", message)
            
            jbCefBrowser?.loadHTML(html)
        } catch (e: Exception) {
            logger.error("Error showing error message", e)
        }
    }
}
