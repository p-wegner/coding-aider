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
import java.text.MessageFormat
import java.util.ResourceBundle

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
    private val resourceBundle = ResourceBundle.getBundle("messages.MarkdownViewerBundle")

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
            loadHTML(createBaseHtml(
                if (this@MarkdownJcefViewer.isDarkTheme) "#2b2b2b" else "#ffffff",
                if (this@MarkdownJcefViewer.isDarkTheme) "#ffffff" else "#000000",
                if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f1f1f1",
                if (this@MarkdownJcefViewer.isDarkTheme) "#555" else "#c1c1c1",
                if (this@MarkdownJcefViewer.isDarkTheme) "#777" else "#a1a1a1",
                if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f5f5f5"
            ))

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
                override fun onLoadError(browser: CefBrowser?, frame: CefFrame?, errorCode: CefLoadHandler.ErrorCode?, errorText: String?, failedUrl: String?) {
                    println("JCEF load error: $errorCode - $errorText for URL: $failedUrl")
                }
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
        currentContent = markdown
        
        if (!contentReady) {
            // Content will be updated when viewer is ready
            println("Content not ready yet, will update when ready")
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
                e.printStackTrace()
                // Fallback to full page reload if JavaScript execution fails
                browser.loadHTML(createHtmlWithContent(html))
            }
        } ?: run {
            println("Warning: Neither fallbackEditor nor jbCefBrowser is initialized")
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
        // Ensure the content div exists and is properly replaced
        if (baseHtml.contains("<div id=\"content\"></div>")) {
            return baseHtml.replace("<div id=\"content\"></div>", "<div id=\"content\">$content</div>")
        } else {
            // If the div isn't found for some reason, insert content before body closing tag
            return baseHtml.replace("</body>", "<div id=\"content\">$content</div></body>")
        }
    }

    fun setDarkTheme(dark: Boolean) {
        if (isDarkTheme != dark) {
            isDarkTheme = dark
            if (currentContent.isNotEmpty()) {
                // Reload with new theme
                jbCefBrowser?.loadHTML(createBaseHtml(
                    if (this@MarkdownJcefViewer.isDarkTheme) "#2b2b2b" else "#ffffff",
                    if (this@MarkdownJcefViewer.isDarkTheme) "#ffffff" else "#000000",
                    if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f1f1f1",
                    if (this@MarkdownJcefViewer.isDarkTheme) "#555" else "#c1c1c1",
                    if (this@MarkdownJcefViewer.isDarkTheme) "#777" else "#a1a1a1",
                    if (this@MarkdownJcefViewer.isDarkTheme) "#1e1e1e" else "#f5f5f5"
                ))
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
            
            return """
            <div class="collapsible-panel expanded">
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
