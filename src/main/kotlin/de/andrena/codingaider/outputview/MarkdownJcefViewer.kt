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

class MarkdownJcefViewer(private val lookupPaths: List<String> = emptyList()) {

    private val mainPanel: JPanel = JPanel(BorderLayout()).apply {
        border = null
        minimumSize = Dimension(200, 100)
        preferredSize = Dimension(600, 400)
        isOpaque = true
        background = if (!JBColor.isBright()) JBColor(0x2B2B2B, 0x2B2B2B) else JBColor.WHITE
    }
    private var jbCefBrowser: JBCefBrowser? = null
    private var isDarkTheme = false
    private var currentContent = ""

    init {
        if (JBCefApp.isSupported()) {
            // Create the JCEF Browser
            jbCefBrowser = JBCefBrowser().apply {
                component.apply {
                    isFocusable = true
                    minimumSize = Dimension(200, 100)
                    background = mainPanel.background
                }
                jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
                    override fun onLoadingStateChange(browser: CefBrowser?, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                        if (!isLoading && browser != null) {
                            // Inject scroll behavior improvements
                            browser.executeJavaScript("""
                                document.documentElement.style.height = '100%';
                                document.body.style.height = '100%';
                                document.body.style.margin = '20px';
                                document.body.style.boxSizing = 'border-box';
                            """.trimIndent(), browser.url, 0)
                        }
                    }
                })
            }
            // Add the browser component to our mainPanel with BorderLayout.CENTER
            mainPanel.add(jbCefBrowser!!.component, BorderLayout.CENTER)
        } else {
            // Fallback if JCEF is not supported
            mainPanel.add(
                javax.swing.JLabel("JCEF is not supported on this IDE/platform."),
                BorderLayout.CENTER
            )
        }
    }

    val component: JComponent
        get() = mainPanel

    fun setMarkdown(markdown: String) {
        if (markdown == currentContent) return
        currentContent = markdown
        
        val html = convertMarkdownToHtml(markdown)
        when (val comp = component) {
            is JEditorPane -> {
                comp.text = html
                comp.caretPosition = 0
            }
            else -> {
                jbCefBrowser?.let { browser ->
                    val encodedHtml = Base64.getEncoder().encodeToString(html.toByteArray(Charsets.UTF_8))
                    browser.loadURL("data:text/html;base64,$encodedHtml")
                }
            }
        }
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

        // Process aider blocks
        html = html.replace(
            Regex("<aider-intention>([\\s\\S]*?)</aider-intention>"),
            "<div class=\"aider-intention\">$1</div>"
        ).replace(
            Regex("<aider-summary>([\\s\\S]*?)</aider-summary>"),
            "<div class=\"aider-summary\">$1</div>"
        )
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
                        border: 3px solid ${if (isDark) "#2c4356" else "#bcd6f5"};
                        color: ${if (isDark) "#a8c7f0" else "#0066cc"};
                    }
                    
                    .aider-summary {
                        background: ${if (isDark) "#2d2d2d" else "#fafafa"};
                        border: 3px solid ${if (isDark) "#454545" else "#e5e5e5"};
                        color: ${if (isDark) "#e8e8e8" else "#2b2b2b"};
                        box-shadow: 0 4px 8px ${if (isDark) "rgba(0,0,0,0.3)" else "rgba(0,0,0,0.1)"};
                        transition: all 0.2s ease;
                    }
                    
                    .aider-summary:hover {
                        border-color: ${if (isDark) "#505050" else "#d0d0d0"};
                        box-shadow: 0 6px 12px ${if (isDark) "rgba(0,0,0,0.4)" else "rgba(0,0,0,0.15)"};
                    }
                    
                    .aider-intention::before,
                    .aider-summary::before {
                        display: block;
                        font-weight: bold;
                        font-size: 17px;
                        margin: -12px -16px 16px -16px;
                        padding: 12px 16px;
                        letter-spacing: 0.5px;
                        border-bottom: 2px solid;
                        background: ${if (isDark) "rgba(255,255,255,0.05)" else "rgba(0,0,0,0.03)"};
                        border-radius: 10px 10px 0 0;
                    }
                    
                    .aider-intention::before {
                        content: "🎯 Intention";
                        border-color: ${if (isDark) "#2c4356" else "#bcd6f5"};
                        color: ${if (isDark) "#88b0e4" else "#0055cc"};
                    }
                    
                    .aider-summary::before {
                        content: "📋 Summary";
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

    private fun processSearchReplaceBlocks(html: String): String {
        return html.replace(
            Regex("""(?s)<pre><code>(.+?)<<<<<<< SEARCH\n(.+?)=======\n(.+?)>>>>>>> REPLACE\n</code></pre>"""),
            { matchResult ->
                val (_, filePath, searchBlock, replaceBlock) = matchResult.groupValues
                """
                <div style="font-family: monospace; padding: 4px 0; margin-top: 16px;">$filePath</div>
                <pre>
                <code class="search-block">$searchBlock</code>
                <div style="background: ${if (!com.intellij.ui.JBColor.isBright()) "#666666" else "#cccccc"}; height: 1px;"></div>
                <code class="replace-block">$replaceBlock</code>
                </pre>
                """.trimIndent()
            }
        )
    }

}
