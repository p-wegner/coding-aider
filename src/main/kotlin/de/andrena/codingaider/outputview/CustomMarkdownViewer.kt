package de.andrena.codingaider.outputview

import com.intellij.util.ui.StartupUiUtil.isDarkTheme
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.html.AttributeProvider
import com.vladsch.flexmark.html.AttributeProviderFactory
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.AttributablePart
import com.vladsch.flexmark.html.renderer.LinkResolverContext
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.html.MutableAttributes
import de.andrena.codingaider.utils.FilePathConverter
import javax.swing.JEditorPane

import com.intellij.openapi.diagnostic.Logger

class CustomMarkdownViewer(private val lookupPaths: List<String> = emptyList()) {
    private val logger = Logger.getInstance(CustomMarkdownViewer::class.java)
    val component: JEditorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        putClientProperty("JEditorPane.honorDisplayProperties", true)
        putClientProperty("html.disable", false)
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
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options)
        .attributeProviderFactory(object : AttributeProviderFactory {
            override fun apply(context: LinkResolverContext): AttributeProvider {
                return TaskListAttributeProvider()
            }

            override fun getAfterDependents(): MutableSet<Class<*>>? = null

            override fun getBeforeDependents(): MutableSet<Class<*>>? = null

            override fun affectsGlobalScope(): Boolean = false
        })
        .build()

    private class TaskListAttributeProvider : AttributeProvider {
        override fun setAttributes(node: Node, part: AttributablePart, attributes: MutableAttributes) {
            if (node is TaskListItem && part == AttributablePart.NODE) {
                val checked = node.isItemDoneMarker
                attributes.replaceValue("data-task-status", if (checked) "[x]" else "[ ]")
            }
        }
    }

    private val hyperlinkHandler: HyperlinkHandler = HyperlinkHandler(lookupPaths)
    private var currentContent = ""
    private var isDarkTheme = false

    companion object {
        private fun getHtmlTemplate(
            bodyBg: String,
            bodyText: String,
            preBg: String,
            preBorder: String,
            preText: String,
            codeColor: String,
            linkColor: String,
            tableBorder: String,
            thBg: String,
            trEvenBg: String,
            content: String
        ): String {
            val style =
                getMarkdownCssStyle(
                    bodyBg,
                    bodyText,
                    preBg,
                    preBorder,
                    preText,
                    codeColor,
                    linkColor,
                    tableBorder,
                    thBg,
                    trEvenBg
                )
            return """
                    <html>
                    <head>
                        $style
                    </head>
                    <body>
                        $content
                    </body>
                    </html>
                """
        }

    }

    fun setDarkTheme(dark: Boolean) {
        isDarkTheme = dark
        if (currentContent.isNotEmpty()) {
            setMarkdownContent(currentContent)
        }
    }

    fun setMarkdownContent(markdown: String) {
        if (markdown == currentContent) return
        currentContent = markdown

        try {
            // Convert file paths to markdown links
            val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
            val basePath = project?.basePath
            var processedMarkdown = FilePathConverter.convertPathsToMarkdownLinks(markdown, basePath)

            // Process aider blocks
            processedMarkdown = processedMarkdown.replace(
                Regex("<aider-intention>([\\s\\S]*?)</aider-intention>"),
                "<div class=\"aider-intention\">$1</div>"
            ).replace(
                Regex("<aider-summary>([\\s\\S]*?)</aider-summary>"),
                "<div class=\"aider-summary\">$1</div>"
            )

            val document = parser.parse(processedMarkdown)
            val html = renderer.render(document)

            val colors = if (isDarkTheme) {
                arrayOf(
                    "#2b2b2b", // body background
                    "#ffffff", // body text
                    "#1e1e1e", // pre background
                    "#666666", // pre border
                    "#ffffff", // pre/code text
                    "#ffffff", // code color
                    "#589df6", // link color
                    "#666666", // table border
                    "#323232", // th background
                    "#363636"  // tr even background
                )
            } else {
                arrayOf(
                    "#ffffff", // body background
                    "#000000", // body text
                    "#f5f5f5", // pre background
                    "#cccccc", // pre border
                    "#000000", // pre/code text
                    "#000000", // code color
                    "#0066cc", // link color
                    "#cccccc", // table border
                    "#e6e6e6", // th background
                    "#f5f5f5"  // tr even background
                )
            }

            val styledHtml = getHtmlTemplate(
                bodyBg = colors[0],
                bodyText = colors[1],
                preBg = colors[2],
                preBorder = colors[3],
                preText = colors[4],
                codeColor = colors[5],
                linkColor = colors[6],
                tableBorder = colors[7],
                thBg = colors[8],
                trEvenBg = colors[9],
                content = html
            )

            // Log the generated HTML for debugging
            logger.info("Generated HTML: $styledHtml")

            // Store current caret position
            val caretPos = component.caretPosition
            component.text = styledHtml
            // Restore caret position if possible
            try {
                component.caretPosition = minOf(caretPos, component.document.length)
            } catch (e: Exception) {
                // Ignore if we can't restore position
            }
        } catch (e: Exception) {
            component.text = """
                <html>
                <body>
                <h1>Error Rendering Markdown</h1>
                <p>There was an error processing the markdown content:</p>
                <pre>${e.message}</pre>
                </body>
                </html>
            """.trimIndent()
        }
    }

    init {
        component.addHyperlinkListener { event ->
            hyperlinkHandler.handleHyperlinkEvent(event)
        }
    }

}

fun getMarkdownCssStyle(
    bodyBg: String = "#ffffff",
    bodyText: String = "#000000",
    preBg: String = "#f5f5f5",
    preBorder: String = "#cccccc",
    preText: String = "#000000",
    codeColor: String = "#000000",
    linkColor: String = "#0066cc",
    tableBorder: String = "#cccccc",
    thBg: String = "#e6e6e6",
    trEvenBg: String = "#f5f5f5"
) = """<style type="text/css">
                                    body { 
                                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; 
                                        margin: 20px;
                                        line-height: 1.6;
                                        background-color: ${bodyBg} !important;
                                        color: ${bodyText} !important;
                                    }
                                    pre { 
                                        background-color: ${preBg} !important; 
                                        padding: 10px; 
                                        border-width: 1px;
                                        border-style: solid;
                                        border-color: ${preBorder} !important;
                                        color: ${preText} !important;
                                        margin: 1em 0;
                                    }
                                    code { 
                                        font-family: "JetBrains Mono", "Courier New", Courier, monospace;
                                        color: ${codeColor};
                                    }
                                    a { 
                                        color: ${linkColor}; 
                                        text-decoration: underline;
                                    }
                                    img { 
                                        width: auto;
                                        height: auto;
                                        max-width: 100%;
                                    }
                                    body {
                                        word-wrap: break-word;
                                    }
                                    pre {
                                        white-space: pre;
                                        overflow-x: auto;
                                    }
                                    table {
                                        border-collapse: collapse;
                                        margin: 15px 0;
                                        width: 100%;
                                    }
                                    th, td {
                                        border: 1px solid ${tableBorder};
                                        padding: 8px;
                                        text-align: left;
                                    }
                                    th {
                                        background: ${thBg};
                                    }
                                    tr:nth-child(even) {
                                        background: ${trEvenBg};
                                    }
                                    /* Headings */
                                    h1, h2, h3, h4, h5, h6 {
                                        margin-top: 24px;
                                        margin-bottom: 16px;
                                        font-weight: bold;
                                        line-height: 1.25;
                                    }
                                    h1 { font-size: 2em; border-bottom: 1px solid ${tableBorder}; }
                                    h2 { font-size: 1.5em; border-bottom: 1px solid ${tableBorder}; }
                                    h3 { font-size: 1.25em; }
                                    h4 { font-size: 1em; }
                                    h5 { font-size: 0.875em; }
                                    h6 { font-size: 0.85em; }
                                    
                                    /* Lists */
                                    ul, ol {
                                        padding-left: 2em;
                                        margin: 1em 0;
                                    }
                                    li { margin: 0.25em 0; }
                                    
                                    /* Ordered Lists specific styling */
                                    ol {
                                        counter-reset: item;
                                        list-style-type: none;
                                    }
                                    ol > li {
                                        counter-increment: item;
                                        position: relative;
                                    }
                                    ol > li:before {
                                        content: counter(item) ".";
                                        position: absolute;
                                        left: -2em;
                                        width: 1.5em;
                                        text-align: right;
                                    }
                                    /* Nested lists */
                                    ol ol {
                                        counter-reset: subitem;
                                    }
                                    ol ol > li {
                                        counter-increment: subitem;
                                    }
                                    ol ol > li:before {
                                        content: counter(item) "." counter(subitem);
                                    }
                                    
                                    /* Task Lists */
                                    .task-list {
                                        list-style-type: none;
                                        padding-left: 0;
                                    }
                                    .task-list-item {
                                        margin: 0.5em 0;
                                        padding-left: 1.5em;
                                        position: relative;
                                    }
                                    .task-list-item::before {
                                        content: attr(data-task-status);
                                        position: absolute;
                                        left: 0;
                                        font-family: monospace;
                                        margin-right: 0.5em;
                                        color: inherit !important;
                                    }
                                    
                                    /* Blockquotes */
                                    blockquote {
                                        margin: 1em 0;
                                        padding: 0 1em;
                                        color: #666666;
                                        border-left: 0.25em solid #ddd;
                                    }
                                    
                                    /* Definition Lists */
                                    dl {
                                        margin: 1em 0;
                                    }
                                    dt {
                                        font-weight: bold;
                                        margin-top: 1em;
                                    }
                                    dd {
                                        margin-left: 2em;
                                    }
                                    
                                    /* Footnotes */
                                    .footnotes {
                                        border-top: 1px solid ${tableBorder};
                                        margin-top: 2em;
                                        padding-top: 1em;
                                    }
                                    
                                    /* Aider Blocks */
                                    .aider-intention {
                                        background: ${if (isDarkTheme) "#2d3748" else "#e6f3ff"};
                                        border-left: 4px solid ${if (isDarkTheme) "#4299e1" else "#3182ce"};
                                        padding: 1em;
                                        margin: 1em 0;
                                        border-radius: 4px;
                                    }
                                    
                                    .aider-summary {
                                        background: ${if (isDarkTheme) "#2d3b2d" else "#f0fff4"};
                                        border-left: 4px solid ${if (isDarkTheme) "#48bb78" else "#38a169"};
                                        padding: 1em;
                                        margin: 1em 0;
                                        border-radius: 4px;
                                    }
                                </style>"""


