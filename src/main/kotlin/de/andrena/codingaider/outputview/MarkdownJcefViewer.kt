package de.andrena.codingaider.outputview

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import org.cef.handler.CefLoadHandler
import org.cef.browser.CefBrowser
import org.cef.network.CefRequest
import org.cef.browser.CefFrame
import javax.swing.JEditorPane
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
import java.nio.charset.StandardCharsets
import javax.swing.SwingUtilities

/**
 * A simplified Markdown viewer component that uses JCEF (Chromium Embedded Framework)
 * with fallback to JEditorPane when JCEF is not available.
 */
class MarkdownJcefViewer(private val lookupPaths: List<String> = emptyList()) {

    private val mainPanel = JPanel(BorderLayout()).apply {
        border = null
        minimumSize = Dimension(200, 100)
        preferredSize = Dimension(600, 400)
        isOpaque = true
        background = if (!JBColor.isBright()) JBColor(0x2B2B2B, 0x2B2B2B) else JBColor.WHITE
    }
    
    // Browser or fallback component
    private var jbCefBrowser: JBCefBrowser? = null
    private var fallbackEditor: JEditorPane? = null
    
    // State tracking
    private var isDarkTheme = !JBColor.isBright()
    private var currentContent = ""
    private var contentReady = false
    
    init {
        initializeViewer()
    }
    
    private fun initializeViewer() {
        try {
            if (JBCefApp.isSupported()) {
                initJcefBrowser()
            } else {
                initFallbackEditor()
            }
        } catch (e: Exception) {
            println("Error initializing markdown viewer: ${e.message}")
            e.printStackTrace()
            initFallbackEditor()
        }
    }
    
    private fun initJcefBrowser() {
        // Create browser with simple load handler
        jbCefBrowser = JBCefBrowser().apply {
            component.apply {
                isFocusable = true
                minimumSize = Dimension(200, 100)
                background = mainPanel.background
            }

            // Load the initial HTML template directly
            loadHTML(createBaseHtml(), "http://aider.local/")

            // Set a simple load handler
            val client: JBCefClient = this.jbCefClient
            client.addLoadHandler(object : CefLoadHandler {
                override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                    // Only act on main frame (main frame has no parent)
                    if (frame != null && frame.parent == null) {
                        contentReady = true
                        if (currentContent.isNotEmpty()) {
                            SwingUtilities.invokeLater {
                                updateContent(currentContent)
                            }
                        }
                    }
                }

                override fun onLoadStart(browser: CefBrowser?, frame: CefFrame?, transitionType: CefRequest.TransitionType?) {}
                override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {}
                override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {}
            }, this.cefBrowser)
        }

        mainPanel.add(jbCefBrowser!!.component, BorderLayout.CENTER)
    }
    
    private fun initFallbackEditor() {
        fallbackEditor = JEditorPane().apply {
            contentType = "text/html; charset=UTF-8"
            isEditable = false
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            putClientProperty("JEditorPane.honorDisplayProperties", true)
            putClientProperty("html.disable", false)
            putClientProperty(JEditorPane.W3C_LENGTH_UNITS, true)
        }
        mainPanel.add(fallbackEditor!!, BorderLayout.CENTER)
        contentReady = true
    }
    
    private fun createBaseHtml(): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                    line-height: 1.6;
                    margin: 0;
                    padding: 20px;
                    background: ${if (isDarkTheme) "#2b2b2b" else "#ffffff"};
                    color: ${if (isDarkTheme) "#ffffff" else "#000000"};
                }
                #content {
                    max-width: 100%;
                    overflow-wrap: break-word;
                }
                pre {
                    white-space: pre-wrap;
                    overflow-x: auto;
                    background: ${if (isDarkTheme) "#1e1e1e" else "#f5f5f5"};
                    padding: 10px;
                    border-radius: 4px;
                }
                code {
                    font-family: 'JetBrains Mono', Consolas, Monaco, 'Courier New', monospace;
                }
            </style>
        </head>
        <body>
            <div id="content"></div>
            <script>
                // Store panel states
                let panelStates = {};
                
                // Function to initialize collapsible panels
                function initCollapsiblePanels() {
                    document.querySelectorAll('.collapsible-panel').forEach(panel => {
                        const header = panel.querySelector('.collapsible-header');
                        const panelId = getPanelId(panel);
                        
                        // Remove existing event listeners to prevent duplicates
                        if (header) {
                            header.removeEventListener('click', togglePanel);
                            header.addEventListener('click', togglePanel);
                            
                            // Restore panel state if it exists
                            if (panelStates[panelId] === false) {
                                panel.classList.remove('expanded');
                                const arrow = header.querySelector('.collapsible-arrow');
                                if (arrow) {
                                    arrow.textContent = '▶';
                                }
                            }
                        }
                    });
                }
                
                // Generate a unique ID for each panel based on its content
                function getPanelId(panel) {
                    const header = panel.querySelector('.collapsible-header');
                    let title = '';
                    if (header) {
                        const titleElement = header.querySelector('.collapsible-title');
                        if (titleElement) {
                            title = titleElement.textContent || '';
                        }
                    }
                    
                    const contentElement = panel.querySelector('.collapsible-content');
                    const content = contentElement ? contentElement.textContent.substring(0, 50) : '';
                    
                    // Use panel's data attribute if available, otherwise generate random ID
                    const randomId = panel.dataset.panelId || Math.random().toString(36).substring(2, 8);
                    
                    // Avoid template literals for better compatibility
                    return title + '-' + content.replace(/\s+/g, '') + '-' + randomId;
                }
                
                // Toggle panel function
                function togglePanel(event) {
                    const panel = this.parentElement;
                    const isExpanded = panel.classList.toggle('expanded');
                    
                    // Update arrow indicator
                    const arrow = this.querySelector('.collapsible-arrow');
                    if (arrow) {
                        arrow.textContent = isExpanded ? '▼' : '▶';
                    }
                    
                    // Store panel state
                    const panelId = getPanelId(panel);
                    panelStates[panelId] = isExpanded;
                }
                
                // Function to update content while preserving panel states
                function updateContent(html) {
                    // Save current scroll position
                    const scrollPosition = window.scrollY;
                    const wasAtBottom = isScrolledToBottom();
                    
                    // Store current panel states before updating
                    storeCurrentPanelStates();
                    
                    // Update content
                    document.getElementById('content').innerHTML = html;
                    
                    // Initialize panels with restored states
                    setTimeout(() => {
                        initCollapsiblePanels();
                        
                        // Restore scroll position or scroll to bottom if we were at bottom
                        if (wasAtBottom) {
                            scrollToBottom();
                        } else {
                            window.scrollTo(0, scrollPosition);
                        }
                    }, 50);
                }
                
                // Check if scrolled to bottom
                function isScrolledToBottom() {
                    return (window.innerHeight + window.scrollY) >= (document.body.offsetHeight - 50);
                }
                
                // Scroll to bottom
                function scrollToBottom() {
                    window.scrollTo(0, document.body.scrollHeight);
                }
                
                // Store current panel states
                function storeCurrentPanelStates() {
                    document.querySelectorAll('.collapsible-panel').forEach(panel => {
                        const panelId = getPanelId(panel);
                        panelStates[panelId] = panel.classList.contains('expanded');
                    });
                }
                
                // Initialize panels when page loads
                document.addEventListener('DOMContentLoaded', initCollapsiblePanels);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    val component: JComponent
        get() = mainPanel

    /**
     * Sets the markdown content to be displayed
     */
    fun setMarkdown(markdown: String) {
        currentContent = markdown
        
        if (!contentReady) {
            // Content will be updated when viewer is ready
            return
        }
        
        updateContent(markdown)
    }
    
    private fun updateContent(markdown: String) {
        val html = convertMarkdownToHtml(markdown)
        
        fallbackEditor?.let { editor ->
            SwingUtilities.invokeLater {
                editor.putClientProperty("charset", StandardCharsets.UTF_8.name())
                editor.text = html
                editor.caretPosition = 0
            }
            return
        }
        
        jbCefBrowser?.let { browser ->
            try {
                // Use a simple JavaScript call to update the content
                val escapedHtml = html.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                
                val script = "updateContent('$escapedHtml');"
                browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
            } catch (e: Exception) {
                println("Error updating browser content: ${e.message}")
                // Fallback to full page reload if JavaScript execution fails
                browser.loadHTML(createHtmlWithContent(html))
            }
        }
    }
    
    private fun createHtmlWithContent(content: String): String {
        val baseHtml = createBaseHtml()
        return baseHtml.replace("<div id=\"content\"></div>", "<div id=\"content\">$content</div>")
    }

    fun setDarkTheme(dark: Boolean) {
        if (isDarkTheme != dark) {
            isDarkTheme = dark
            if (currentContent.isNotEmpty()) {
                // Reload with new theme
                jbCefBrowser?.loadHTML(createBaseHtml())
                setMarkdown(currentContent)
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
        val basePath = project?.basePath
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

        // Apply CSS styles
        val styledHtml = """
        <style>
            /* Base styles */
            body { 
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                line-height: 1.6;
                background: ${colors["bodyBg"]};
                color: ${colors["bodyText"]};
                margin: 20px;
                padding: 0;
            }
            
            /* Code blocks */
            pre {
                background: ${colors["preBg"]};
                border: 1px solid ${colors["preBorder"]};
                padding: 15px;
                margin: 15px 0;
                border-radius: 6px;
                overflow-x: auto;
                white-space: pre-wrap;
            }
            
            pre code {
                font-family: 'JetBrains Mono', Consolas, Monaco, 'Courier New', monospace;
                font-size: 14px;
                tab-size: 4;
            }
            
            /* Search/Replace blocks */
            .search-block {
                background: ${colors["searchBg"]};
                color: ${colors["searchText"]};
                padding: 8px 12px;
                border-radius: 4px 4px 0 0;
                margin: 0;
                border-bottom: 1px solid ${colors["preBorder"]};
                display: block;
            }
            
            .replace-block {
                background: ${colors["replaceBg"]};
                color: ${colors["replaceText"]};
                padding: 8px 12px;
                border-radius: 0 0 4px 4px;
                margin: 0;
                display: block;
            }
            
            /* Edit format panels */
            .edit-format-panel {
                border: 1px solid ${if (isDarkTheme) "#555" else "#ddd"};
                border-radius: 6px;
                margin: 15px 0;
                overflow: hidden;
            }
            
            .edit-format {
                background: ${if (isDarkTheme) "#3c3f41" else "#f0f0f0"};
                font-weight: bold;
                border-bottom: 1px solid ${if (isDarkTheme) "#555" else "#ddd"};
            }
            
            .edit-format-content {
                margin: 0;
                padding: 0;
            }
            
            /* Aider blocks */
            .aider-intention, .aider-summary {
                border-radius: 6px;
                padding: 12px;
                margin: 15px 0;
                font-size: 14px;
                line-height: 1.5;
            }
            
            .aider-intention {
                background: ${colors["intentionBg"]};
                border: 1px solid ${colors["intentionBorder"]};
                color: ${colors["intentionText"]};
            }
            
            .aider-summary {
                background: ${colors["summaryBg"]};
                border: 1px solid ${colors["summaryBorder"]};
                color: ${colors["summaryText"]};
            }
            
            /* Collapsible panels */
            .collapsible-panel {
                border: 1px solid ${colors["preBorder"]};
                border-radius: 6px;
                margin: 15px 0;
                overflow: hidden;
            }
            
            .collapsible-header {
                background: ${colors["preBg"]};
                padding: 10px 15px;
                cursor: pointer;
                display: flex;
                justify-content: space-between;
                align-items: center;
            }
            
            .collapsible-title {
                font-weight: bold;
            }
            
            .collapsible-content {
                padding: 0;
                max-height: 0;
                overflow: hidden;
                transition: max-height 0.3s ease-out, padding 0.3s ease-out;
            }
            
            .collapsible-panel.expanded .collapsible-content {
                max-height: 10000px; /* Increased to handle larger content */
                padding: 10px 15px;
            }
            
            /* Ensure arrows are visible */
            .collapsible-arrow {
                font-weight: bold;
                margin-left: 10px;
                user-select: none;
            }
            
            /* File path styling */
            .file-path {
                font-family: monospace;
                padding: 5px 10px;
                background: ${colors["preBg"]};
                border-bottom: 1px solid ${colors["preBorder"]};
            }
            
            /* Lists in aider blocks */
            .aider-intention ul, .aider-summary ul {
                padding-left: 20px;
            }
            
            .aider-intention li, .aider-summary li {
                margin: 5px 0;
            }
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
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    /**
     * Processes search/replace blocks and other special formatting
     */
    private fun processSearchReplaceBlocks(html: String): String {
        var processedHtml = html

        // Helper function to create collapsible panels
        fun createCollapsiblePanel(title: String, content: String, cssClass: String = "", isEscaped: Boolean = true): String {
            val contentHtml = if (isEscaped) "<pre><code>${escapeHtml(content.trim())}</code></pre>" else content.trim()
            val panelId = "panel-${title.replace(" ", "-").lowercase()}-${content.hashCode()}"
            
            return """
            <div class="collapsible-panel expanded" data-panel-id="$panelId">
                <div class="collapsible-header $cssClass">
                    <span class="collapsible-title">$title</span>
                    <span class="collapsible-arrow">▼</span>
                </div>
                <div class="collapsible-content">
                    $contentHtml
                </div>
            </div>
            """.trimIndent()
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
        val searchReplacePattern = Regex("""(?m)^([^\n]+?)\n```[^\n]*\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```""", 
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
