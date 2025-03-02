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
import java.nio.charset.StandardCharsets

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
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
            <meta http-equiv="Content-Security-Policy" content="default-src 'self' 'unsafe-inline' data:"/>
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

        // Ensure proper UTF-8 encoding for base HTML
        val encodedBase = Base64.getEncoder().encodeToString(baseHtml.toByteArray(StandardCharsets.UTF_8))
        jbCefBrowser.loadURL("data:text/html;base64,$encodedBase")
    }
    private fun createFallbackComponent() {
        // The user does not have JCEF support, fallback to JEditorPane
        fallbackEditor = JEditorPane().apply {
            contentType = "text/html; charset=UTF-8"
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
        fallbackEditor?.let { editor ->
            editor.putClientProperty("charset", StandardCharsets.UTF_8.name())
            editor.text = html
            editor.caretPosition = 0
            return
        }

        // Otherwise, use JCEF and inject the HTML snippet
        if (!isBasePageLoaded) {
            loadBasePage()
            isBasePageLoaded = true
        }

        // Ensure proper UTF-8 encoding for content HTML
        val encodedHtml = Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))

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

            // Store expansion states before update
            var expandedPanels = Array.from(document.querySelectorAll('.collapsible-panel.expanded'))
               .map(panel => panel.querySelector('.collapsible-content').textContent.trim());
            
            // Inject new HTML
            var decoded = atob('$encodedHtml');
            container.innerHTML = decoded;
            
            // Restore expansion states
            expandedPanels.forEach(content => {
                var panels = Array.from(document.querySelectorAll('.collapsible-panel'));
                panels.forEach(panel => {
                    if (panel.querySelector('.collapsible-content').textContent.trim() === content) {
                        panel.classList.add('expanded');
                    }
                });
            });

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

        // Process aider blocks with proper markdown rendering
        html = html.replace(
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
                        padding: 20px;
                        margin: 20px 0;
                        border-radius: 10px;
                        box-shadow: 0 3px 6px ${if (isDark) "rgba(0,0,0,0.4)" else "rgba(0,0,0,0.15)"};
                        overflow-x: auto;
                        max-width: 100%;
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        box-sizing: border-box;
                        width: calc(100% - 40px);
                        transition: all 0.2s ease;
                    }

                    pre:hover {
                        box-shadow: 0 5px 12px ${if (isDark) "rgba(0,0,0,0.5)" else "rgba(0,0,0,0.2)"};
                        transform: translateY(-1px);
                    }
                    
                    pre code {
                        font-family: 'JetBrains Mono', Consolas, Monaco, 'Courier New', monospace;
                        font-size: 15px;
                        line-height: 1.6;
                        tab-size: 4;
                        letter-spacing: 0.3px;
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
                        border-radius: 6px;
                        padding: 8px;
                        margin: 8px 0;
                        position: relative;
                        font-size: 13px;
                        line-height: 1.2;
                        box-shadow: 0 1px 4px ${if (isDark) "rgba(0,0,0,0.2)" else "rgba(0,0,0,0.08)"};
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        overflow-wrap: break-word;
                    }
                    
                    .aider-intention {
                        background: ${if (isDark) "#1a2733" else "#f0f7ff"};
                        border: 1px solid ${if (isDark) "#2c4356" else "#bcd6f5"};
                        color: ${if (isDark) "#a8c7f0" else "#0066cc"};
                        padding: 12px;
                        margin: 12px 0;
                    }
                    
                    .aider-summary {
                        background: ${if (isDark) "#2d2d2d" else "#fafafa"};
                        border: 1px solid ${if (isDark) "#454545" else "#e5e5e5"};
                        color: ${if (isDark) "#e8e8e8" else "#2b2b2b"};
                        padding: 12px;
                        margin: 12px 0;
                        transition: all 0.2s ease;
                    }
                    
                    .aider-summary:hover {
                        border-color: ${if (isDark) "#505050" else "#d0d0d0"};
                        box-shadow: 0 2px 6px ${if (isDark) "rgba(0,0,0,0.3)" else "rgba(0,0,0,0.1)"};
                        transform: none; /* Remove transform to prevent movement */
                    }

                    .collapsible-panel {
                        background: ${if (isDark) "#2d2d2d" else "#f8f8f8"};
                        border: 1px solid ${if (isDark) "#404040" else "#e0e0e0"};
                        border-radius: 8px;
                        margin: 16px 0;
                        overflow: hidden;
                        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                    }

                    .collapsible-panel.expanded {
                        box-shadow: 0 2px 8px ${if (isDark) "rgba(0,0,0,0.3)" else "rgba(0,0,0,0.1)"};
                    }

                    .collapsible-header {
                        background: ${if (isDark) "#383838" else "#f0f0f0"};
                        padding: 25px 15px 10px;
                        cursor: pointer;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        user-select: none;
                        transition: background-color 0.2s;
                        position: relative;
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
                        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                        padding: 0;
                        margin: 0;
                        opacity: 0;
                    }

                    .collapsible-panel.expanded .collapsible-content {
                        opacity: 1;
                    }

                    .collapsible-panel.expanded .collapsible-arrow {
                        transform: rotate(180deg);
                    }

                    .collapsible-panel.expanded .collapsible-content {
                        max-height: none;
                        padding: 15px;
                        overflow: visible;
                    }
                    
                    
                    .collapsible-header.intention {
                        position: relative;
                    }
                    
                    .collapsible-header.intention {
                        background: ${if (isDark) "rgba(136,176,228,0.1)" else "rgba(0,85,204,0.05)"};
                        color: ${if (isDark) "#88b0e4" else "#0055cc"};
                        border-bottom: 1px solid ${if (isDark) "rgba(136,176,228,0.2)" else "rgba(0,85,204,0.1)"};
                    }
                    
                    .collapsible-header.summary {
                        position: relative;
                    }
                    
                    .collapsible-header.summary {
                        background: ${if (isDark) "rgba(204,204,204,0.1)" else "rgba(102,102,102,0.05)"};
                        color: ${if (isDark) "#cccccc" else "#666666"};
                        border-bottom: 1px solid ${if (isDark) "rgba(204,204,204,0.2)" else "rgba(102,102,102,0.1)"};
                    }
                    
                    .collapsible-header.code {
                        position: relative;
                    }
                    
                    .collapsible-header.code::before {
                        content: "Code Changes";
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        padding: 4px 15px;
                        background: ${if (isDark) "rgba(169,183,198,0.1)" else "rgba(51,51,51,0.05)"};
                        color: ${if (isDark) "#a9b7c6" else "#333333"};
                        font-size: 12px;
                        font-weight: 600;
                        border-bottom: 1px solid ${if (isDark) "rgba(169,183,198,0.2)" else "rgba(51,51,51,0.1)"};
                    }

                    .code-block {
                        background: ${if (isDark) "#2d2d2d" else "#f8f8f8"};
                        border-radius: 6px;
                        margin: 8px 0;
                        overflow: hidden;
                    }

                    .code-block .file-path {
                        background: ${if (isDark) "#383838" else "#f0f0f0"};
                        color: ${if (isDark) "#cccccc" else "#666666"};
                        padding: 8px 12px;
                        font-family: monospace;
                        font-size: 13px;
                        border-bottom: 1px solid ${if (isDark) "#454545" else "#e5e5e5"};
                    }

                    .code-block pre {
                        margin: 0;
                        border-radius: 0 0 6px 6px;
                    }
                    
                    .aider-intention ul,
                    .aider-intention ol,
                    .aider-summary ul,
                    .aider-summary ol {
                        margin: 4px 0;
                        padding-left: 16px;
                    }
                    
                    .aider-intention li,
                    .aider-summary li {
                        margin: 2px 0;
                        padding: 0;
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
                        margin: 8px 0;
                        line-height: 1.4;
                    }
                    
                    .aider-intention code,
                    .aider-summary code {
                        background: ${if (isDark) "rgba(255,255,255,0.08)" else "rgba(0,0,0,0.04)"};
                        padding: 2px 4px;
                        border-radius: 2px;
                        font-family: monospace;
                        font-size: 12px;
                        line-height: 1;
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
        val commandPattern = """<aider-command>\s*(.*?)\s*</aider-command>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        processedHtml = processedHtml.replace(commandPattern) { matchResult ->
            """
            <div class="collapsible-panel">
                <div class="collapsible-header" onclick="this.parentElement.classList.toggle('expanded')">
                    <span class="collapsible-title">Aider Command</span>
                    <span class="collapsible-arrow"> ^ </span>
                </div>
                <div class="collapsible-content">
                    <pre><code>${escapeHtml(matchResult.groupValues[1].trim())}</code></pre>
                </div>
            </div>
            """.trimIndent()
        }

        // Process intention blocks with preserved formatting
        processedHtml = processedHtml.replace(
            Regex("""<div class="aider-intention">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)) { matchResult ->
                """
                <div class="collapsible-panel expanded">
                    <div class="collapsible-header intention" onclick="this.parentElement.classList.toggle('expanded')">
                        <span class="collapsible-title">Intention</span>
                        <span class="collapsible-arrow">^</span>
                    </div>
                    <div class="collapsible-content">
                        ${matchResult.groupValues[1].trim()}
                    </div>
                </div>
                """.trimIndent()
        }
        
        // Process summary blocks with preserved formatting
        processedHtml = processedHtml.replace(
            Regex("""<div class="aider-summary">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)) { matchResult ->
                """
                <div class="collapsible-panel expanded">
                    <div class="collapsible-header summary" onclick="this.parentElement.classList.toggle('expanded')">
                        <span class="collapsible-title">Summary</span>
                        <span class="collapsible-arrow">^</span>
                    </div>
                    <div class="collapsible-content">
                        ${matchResult.groupValues[1].trim()}
                    </div>
                </div>
                """.trimIndent()
        }

        // Process file paths followed by code blocks with search/replace content
        val filePathCodeBlockPattern = Regex("""(?m)^([^\n]+?)\n```[^\n]*\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```""", RegexOption.DOT_MATCHES_ALL)
        processedHtml = processedHtml.replace(filePathCodeBlockPattern) { matchResult ->
            val (filePath, searchBlock, replaceBlock) = matchResult.destructured
            """
            <div class="collapsible-panel expanded">
                <div class="collapsible-header code" onclick="this.parentElement.classList.toggle('expanded')">
                    <span class="collapsible-title">${escapeHtml(filePath.trim())}</span>
                    <span class="collapsible-arrow">^</span>
                </div>
                <div class="collapsible-content">
                    <div class="code-block">
                        <div class="file-path">${escapeHtml(filePath.trim())}</div>
                        <pre>
                            <code class="search-block">${escapeHtml(searchBlock.trim())}</code>
                            <code class="replace-block">${escapeHtml(replaceBlock.trim())}</code>
                        </pre>
                    </div>
                </div>
            </div>
            """.trimIndent()
        }

        // Process remaining standard search/replace blocks
        return processedHtml.replace(
            Regex("""(?s)<pre><code>(.+?)<<<<<<< SEARCH\n(.*?)=======\n(.*?)>>>>>>> REPLACE\n</code></pre>"""),
            { matchResult ->
                val (_, filePath, searchBlock, replaceBlock) = matchResult.groupValues
                """
                <div class="collapsible-panel expanded">
                    <div class="collapsible-header code" onclick="this.parentElement.classList.toggle('expanded')">
                        <span class="collapsible-title">${escapeHtml(filePath)}</span>
                        <span class="collapsible-arrow">^</span>
                    </div>
                    <div class="collapsible-content">
                        <pre>
                        <code class="search-block">${escapeHtml(searchBlock)}</code>
                        <div class="divider"></div>
                        <code class="replace-block">${escapeHtml(replaceBlock)}</code>
                        </pre>
                    </div>
                </div>
                """.trimIndent()
            }
        )
    }

}
