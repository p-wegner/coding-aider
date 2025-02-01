package de.andrena.codingaider.outputview

import com.intellij.ui.jcef.*
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
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.BorderLayout
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.util.data.MutableDataSet
import de.andrena.codingaider.utils.FilePathConverter
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter

class MarkdownJcefViewer(private val lookupPaths: List<String> = emptyList()) {

    private val mainPanel: JPanel = JPanel(BorderLayout()).apply {
        border = null
        minimumSize = Dimension(200, 100)
        preferredSize = Dimension(600, 400)
        isOpaque = true
        background = if (!JBColor.isBright()) JBColor(0x2B2B2B, 0x2B2B2B) else JBColor.WHITE
    }
    private lateinit var jbCefBrowser: JBCefBrowser

    private var fallbackEditor: JEditorPane? = null
    private var isDarkTheme = false
    private var currentContent = ""
    private var isBasePageLoaded = false
    init {
        try {
            if (JBCefApp.isSupported()) {
                // Create the JCEF Browser
                jbCefBrowser = JBCefBrowser().apply {
                    component.apply {
                        isFocusable = true
                        minimumSize = Dimension(200, 100)
                        background = mainPanel.background
                    }
                }
                // Add the browser component to our mainPanel with BorderLayout.CENTER
                mainPanel.add(jbCefBrowser.component, BorderLayout.CENTER)
                // Load the base page exactly once here in init
                jbCefBrowser.cefBrowser.client.addLoadHandler(object : CefLoadHandlerAdapter() {
                    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                        // Make sure it's the main frame finishing load
                        if (frame?.isMain == true) {
                            // Now the #markdown-container definitely exists
                            // => do your injection
                            setMarkdown(currentContent)
                        }
                    }
                })
                loadBasePage()
                isBasePageLoaded = true
            } else {
                createFallbackComponent()
            }
        } catch (e: Exception) {
            println("Error initializing JCEF browser: ${e.message}")
            e.printStackTrace()
            createFallbackComponent()
        }
    }
    private fun loadBasePage() {
        val baseHtml = """
        <html>
          <head>
            <meta charset="UTF-8"/>
            <title>Markdown Viewer</title>
            <style>
              html, body {
                margin: 0; 
                padding: 0;
                /* 100% height to allow a child to fill */
                height: 100%;
                box-sizing: border-box;
                overflow: hidden; /* We'll let container do the scrolling */
              }
              #markdown-container {
                box-sizing: border-box;
                padding: 20px;
                /* Take up the full visible space of the browser */
                height: 100%;
                /* Make this element scrollable */
                overflow-y: auto;
                /* If you want horizontal scrolling only if needed:
                   overflow-x: auto; 
                */
              }
            </style>
          </head>
          <body>
            <div id="markdown-container"></div>
          </body>
        </html>
    """.trimIndent()

        val encodedBase = Base64.getEncoder().encodeToString(baseHtml.toByteArray(Charsets.UTF_8))
        jbCefBrowser.loadURL("data:text/html;base64,$encodedBase")
    }
    private fun createFallbackComponent() {
        // The user does not have JCEF support, fallback to JEditorPane
        fallbackEditor = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            putClientProperty("JEditorPane.honorDisplayProperties", true)
            putClientProperty("html.disable", false)
            putClientProperty(JEditorPane.W3C_LENGTH_UNITS, true)
        }
        mainPanel.add(fallbackEditor, BorderLayout.CENTER)
    }


    val component: JComponent
        get() = mainPanel

    fun setMarkdown(markdown: String) {
        currentContent = markdown

        val html = convertMarkdownToHtml(markdown)

        // If fallback editor is in use
        fallbackEditor?.let {
            it.text = html
            it.caretPosition = 0
            return
        }

        // Otherwise, use JCEF and inject the HTML snippet
        if (!isBasePageLoaded) {
            loadBasePage()
            isBasePageLoaded = true
        }

        val encodedHtml = Base64.getEncoder().encodeToString(html.toByteArray(Charsets.UTF_8))

        // We'll do the wasAtBottom check, replace container content, and scroll if needed.
        val script = """
        (function() {
            var container = document.getElementById('markdown-container');
            if (!container) return;

            // Are we close to bottom already?
            var scrollPos = container.scrollTop;
            var clientH = container.clientHeight;
            var scrollH = container.scrollHeight;
            var nearBottom = (scrollPos + clientH >= scrollH - 5);

            // Inject new HTML
            var decoded = atob('$encodedHtml');
            container.innerHTML = decoded;

            // If we were at the bottom before, jump to bottom again
            if (nearBottom) {
                container.scrollTop = container.scrollHeight;
            }
        })();
    """.trimIndent()

        jbCefBrowser.cefBrowser.executeJavaScript(script, jbCefBrowser.cefBrowser.url, 0)
    }

    fun setDarkTheme(dark: Boolean) {
        isDarkTheme = dark
        if (currentContent.isNotEmpty()) {
            setMarkdown(currentContent)
        }
    }

    private val options = MutableDataSet().apply {
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
        val parser = Parser.builder(options).build()
        val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
        val basePath = project?.basePath
        var processedMarkdown = FilePathConverter.convertPathsToMarkdownLinks(markdown, basePath)
        val document = parser.parse(processedMarkdown)

        val renderer = HtmlRenderer.builder(options).build()
        var html = renderer.render(document)

        // Process aider blocks with proper whitespace and HTML escaping
        html = html.replace(
            Regex("(?s)<aider-intention>\\s*(.*?)\\s*</aider-intention>")) { matchResult ->
                "<div class=\"aider-intention\">${escapeHtml(matchResult.groupValues[1])}</div>"
        }.replace(
            Regex("(?s)<aider-summary>\\s*(.*?)\\s*</aider-summary>")) { matchResult ->
                "<div class=\"aider-summary\">${escapeHtml(matchResult.groupValues[1])}</div>"
        }
        val isDark = !com.intellij.ui.JBColor.isBright()
        val colors = if (isDark) {
            mapOf(
                "bodyBg" to "#2b2b2b",
                "bodyText" to "#ffffff",
                "preBg" to "#1e1e1e",
                "preBorder" to "#666666",
                "searchBg" to "#362a1e",
                "replaceBg" to "#1e3626",
                "searchText" to "#ff8c7c",
                "replaceText" to "#7cff8c"
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
                "replaceText" to "#28a745"
            )
        }

        return """
            <html>
            <head>
                <style id="theme-styles">
                    :root {
                        --body-bg: ${if (isDark) "#2b2b2b" else "#ffffff"};
                        --body-text: ${if (isDark) "#ffffff" else "#000000"};
                        --intention-bg: ${if (isDark) "#1a2733" else "#f0f7ff"};
                        --intention-border: ${if (isDark) "#2c4356" else "#bcd6f5"};
                        --intention-text: ${if (isDark) "#589df6" else "#0066cc"};
                        --summary-bg: ${if (isDark) "#2b2b2b" else "#f7f7f7"};
                        --summary-border: ${if (isDark) "#404040" else "#e0e0e0"};
                        --summary-text: ${if (isDark) "#cccccc" else "#333333"};
                    }
                    body { 
                        font-family: sans-serif;
                        margin: 20px;
                        line-height: 1.6;
                        background: var(--body-bg);
                        color: var(--body-text);
                        max-width: 100%;
                        overflow-x: hidden;
                        box-sizing: border-box;
                    }
                    pre {
                        background: ${colors["preBg"]};
                        border: 1px solid ${colors["preBorder"]};
                        padding: 16px;
                        margin: 16px 0;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px ${if (isDark) "rgba(0,0,0,0.3)" else "rgba(0,0,0,0.1)"};
                        overflow-x: auto;
                        max-width: 100%;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        box-sizing: border-box;
                        width: calc(100% - 32px); /* Account for padding */
                    }
                    
                    pre code {
                        font-family: 'JetBrains Mono', Consolas, Monaco, 'Courier New', monospace;
                        font-size: 14px;
                        line-height: 1.5;
                        tab-size: 4;
                    }
                    .search-block {
                        background: ${colors["searchBg"]};
                        color: ${colors["searchText"]};
                        padding: 8px 12px;
                        border-radius: 6px 6px 0 0;
                        margin: 0;
                        border-bottom: 2px solid ${if (isDark) "#444" else "#ddd"};
                    }
                    .replace-block {
                        background: ${colors["replaceBg"]};
                        color: ${colors["replaceText"]};
                        padding: 8px 12px;
                        border-radius: 0 0 6px 6px;
                        margin: 0;
                    }
                    .aider-intention, .aider-summary {
                        border-radius: 12px;
                        padding: 24px;
                        margin: 32px 0;
                        position: relative;
                        font-size: 15px;
                        line-height: 1.7;
                        box-shadow: 0 4px 12px ${if (isDark) "rgba(0,0,0,0.4)" else "rgba(0,0,0,0.15)"};
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                    }
                    
                    .aider-intention {
                        background: ${if (isDark) "#1a2733" else "#f0f7ff"};
                        border: 2px solid ${if (isDark) "#2c4356" else "#bcd6f5"};
                        color: ${if (isDark) "#a8c7f0" else "#0066cc"};
                        box-shadow: 0 2px 8px ${if (isDark) "rgba(0,0,0,0.3)" else "rgba(0,0,0,0.1)"};
                    }
                    
                    .aider-summary {
                        background: ${if (isDark) "#2d2d2d" else "#fafafa"};
                        border: 2px solid ${if (isDark) "#454545" else "#e5e5e5"};
                        color: ${if (isDark) "#e8e8e8" else "#2b2b2b"};
                        box-shadow: 0 2px 8px ${if (isDark) "rgba(0,0,0,0.2)" else "rgba(0,0,0,0.08)"};
                        transition: all 0.2s ease;
                    }
                    
                    .aider-summary:hover {
                        border-color: ${if (isDark) "#505050" else "#d0d0d0"};
                        box-shadow: 0 6px 12px ${if (isDark) "rgba(0,0,0,0.4)" else "rgba(0,0,0,0.15)"};
                    }

                    .collapsible-panel {
                        background: ${if (isDark) "#2d2d2d" else "#f8f8f8"};
                        border: 1px solid ${if (isDark) "#404040" else "#e0e0e0"};
                        border-radius: 6px;
                        margin: 10px 0;
                        overflow: hidden;
                    }

                    .collapsible-header {
                        background: ${if (isDark) "#383838" else "#f0f0f0"};
                        padding: 10px 15px;
                        cursor: pointer;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        user-select: none;
                        transition: background-color 0.2s;
                    }

                    .collapsible-header:hover {
                        background: ${if (isDark) "#404040" else "#e8e8e8"};
                    }

                    .collapsible-title {
                        font-weight: bold;
                        color: ${if (isDark) "#cccccc" else "#333333"};
                    }

                    .collapsible-arrow {
                        transition: transform 0.3s;
                    }

                    .collapsible-content {
                        max-height: 0;
                        overflow: hidden;
                        transition: max-height 0.3s ease-out;
                        padding: 0 15px;
                    }

                    .collapsible-panel.expanded .collapsible-arrow {
                        transform: rotate(180deg);
                    }

                    .collapsible-panel.expanded .collapsible-content {
                        max-height: 500px;
                        padding: 15px;
                    }
                    
                    .aider-intention::before,
                    .aider-summary::before {
                        display: block;
                        font-weight: bold;
                        font-size: 17px;
                        margin: -24px -24px 16px -24px;
                        padding: 16px 24px;
                        letter-spacing: 0.5px;
                        border-bottom: 2px solid;
                        background: ${if (isDark) "rgba(255,255,255,0.05)" else "rgba(0,0,0,0.03)"};
                        border-radius: 12px 12px 0 0;
                    }
                    
                    .aider-intention::before {
                        content: "Intention";
                        border-color: ${if (isDark) "#2c4356" else "#bcd6f5"};
                        color: ${if (isDark) "#88b0e4" else "#0055cc"};
                    }
                    
                    .aider-summary::before {
                        content: "Summary";
                        border-color: ${if (isDark) "#404040" else "#e0e0e0"};
                        color: ${if (isDark) "#cccccc" else "#666666"};
                    }
                    
                    .aider-intention ul,
                    .aider-intention ol,
                    .aider-summary ul,
                    .aider-summary ol {
                        margin: 8px 0;
                        padding-left: 24px;
                    }
                    
                    .aider-intention li,
                    .aider-summary li {
                        margin: 4px 0;
                    }
                    
                    .aider-intention ul li::marker {
                        color: ${if (isDark) "#88b0e4" else "#0055cc"};
                        font-size: 1.1em;
                    }
                    
                    .aider-summary ul li::marker {
                        color: ${if (isDark) "#808080" else "#666666"};
                        font-size: 1.1em;
                    }
                    
                    .aider-intention p,
                    .aider-summary p {
                        margin: 12px 0;
                        line-height: 1.8;
                    }
                    
                    .aider-intention code,
                    .aider-summary code {
                        background: ${if (isDark) "rgba(255,255,255,0.1)" else "rgba(0,0,0,0.05)"};
                        padding: 2px 6px;
                        border-radius: 4px;
                        font-family: monospace;
                    }
                    
                    /* Custom scrollbar styling */
                    ::-webkit-scrollbar {
                        width: 12px;
                        height: 12px;
                    }
                    
                    ::-webkit-scrollbar-track {
                        background: ${if (isDark) "#3c3f41" else "#f0f0f0"};
                        border-radius: 6px;
                    }
                    
                    ::-webkit-scrollbar-thumb {
                        background: ${if (isDark) "#585858" else "#c4c4c4"};
                        border: 3px solid ${if (isDark) "#3c3f41" else "#f0f0f0"};
                        border-radius: 6px;
                        min-height: 40px;
                    }
                    
                    ::-webkit-scrollbar-thumb:hover {
                        background: ${if (isDark) "#666666" else "#a8a8a8"};
                    }
                    
                    ::-webkit-scrollbar-thumb:active {
                        background: ${if (isDark) "#787878" else "#919191"};
                    }
                    
                    ::-webkit-scrollbar-corner {
                        background: ${if (isDark) "#3c3f41" else "#f0f0f0"};
                    }
                </style>
            </head>
            <body>
                ${processSearchReplaceBlocks(html)}
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    /**
     * Processes the HTML content to add special formatting for search/replace blocks and collapsible panels.
     * 
     * @param html The input HTML content to process
     * @return The processed HTML with formatted blocks and collapsible panels
     */
    private fun processSearchReplaceBlocks(html: String): String {
        var processedHtml = html

        // Wrap initial command in collapsible panel if present
        // This creates an expandable/collapsible section for the initial command
        // to improve readability of the output
        val commandPattern = """<aider-command>\s*(.*?)</aider-command>""".toRegex()
        processedHtml = processedHtml.replace(commandPattern) { matchResult ->
            """
            <div class="collapsible-panel">
                <div class="collapsible-header" onclick="this.parentElement.classList.toggle('expanded')">
                    <span class="collapsible-title">Initial Command</span>
                    <span class="collapsible-arrow">▼</span>
                </div>
                <div class="collapsible-content">
                    <code>${matchResult.groupValues[1]}</code>
                </div>
            </div>
            """.trimIndent()
        }

        // Process search/replace blocks
        return processedHtml.replace(
            Regex("""(?s)<pre><code>(.+?)<<<<<<< SEARCH\n(.*?)=======\n(.*?)>>>>>>> REPLACE\n</code></pre>"""),
            { matchResult ->
                val (_, filePath, searchBlock, replaceBlock) = matchResult.groupValues
                """
                <div class="file-path">${escapeHtml(filePath)}</div>
                <pre>
                <code class="search-block">${escapeHtml(searchBlock)}</code>
                <div class="divider"></div>
                <code class="replace-block">${escapeHtml(replaceBlock)}</code>
                </pre>
                """.trimIndent()
            }
        )
    }

}
