package de.andrena.codingaider.outputview

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

@Deprecated("Use MarkdownJcefViewer instead")
class CustomMarkdownViewer(private val lookupPaths: List<String> = emptyList()) {
    val component: JEditorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        putClientProperty("JEditorPane.honorDisplayProperties", true)
        putClientProperty("html.disable", false)
        putClientProperty(JEditorPane.W3C_LENGTH_UNITS, true)
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

    companion object {}

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
                mapOf(
                    "bodyBg" to "#2b2b2b",
                    "bodyText" to "#ffffff", 
                    "preBg" to "#1e1e1e",
                    "preBorder" to "#666666",
                    "preText" to "#ffffff",
                    "codeColor" to "#ffffff",
                    "linkColor" to "#589df6",
                    "tableBorder" to "#666666",
                    "thBg" to "#323232",
                    "trEvenBg" to "#363636",
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
                    "preText" to "#000000",
                    "codeColor" to "#000000",
                    "linkColor" to "#0066cc",
                    "tableBorder" to "#cccccc",
                    "thBg" to "#e6e6e6",
                    "trEvenBg" to "#f5f5f5",
                    "searchBg" to "#ffedeb",
                    "replaceBg" to "#ebffed",
                    "searchText" to "#d73a49",
                    "replaceText" to "#28a745"
                )
            }

            val styledHtml = """
                <html>
                <head>
                    <style type="text/css">
                        body { 
                            font-family: sans-serif;
                            margin: 20px;
                            line-height: 1.6;
                            background: ${colors["bodyBg"]};
                            color: ${colors["bodyText"]};
                        }
                        pre {
                            background: ${colors["preBg"]};
                            border: solid 1px ${colors["preBorder"]};
                            padding: 10px;
                            margin: 0;
                        }
                        .search-block {
                            background: ${colors["searchBg"]};
                            color: ${colors["searchText"]};
                            padding: 4px 8px;
                            margin: 0;
                        }
                        .replace-block {
                            background: ${colors["replaceBg"]};
                            color: ${colors["replaceText"]};
                            padding: 4px 8px;
                            margin: 0;
                        }
                        .aider-intention {
                            background: #f0f7ff;
                            border: 1px solid #bcd6f5;
                            border-radius: 4px;
                            padding: 12px;
                            margin: 12px 0;
                            color: #0066cc;
                        }
                        .aider-summary {
                            background: #f7f7f7;
                            border: 1px solid #e0e0e0;
                            border-radius: 4px;
                            padding: 12px;
                            margin: 12px 0;
                            color: #333333;
                        }
                        body.dark-theme .aider-intention {
                            background: #1a2733;
                            border-color: #2c4356;
                            color: #589df6;
                        }
                        body.dark-theme .aider-summary {
                            background: #2b2b2b;
                            border-color: #404040;
                            color: #cccccc;
                        }
                        .divider {
                            background: ${colors["preBorder"]};
                            height: 1px;
                            margin: 0;
                            padding: 0;
                        }
                        .file-path {
                            font-family: monospace;
                            padding: 4px 0;
                            margin-top: 16px;
                            color: ${colors["bodyText"]};
                        }
                    </style>
                </head>
                <body>
                    ${processSearchReplaceBlocks(html)}
                </body>
                </html>
            """.trimIndent()


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


    private fun processSearchReplaceBlocks(html: String): String {
        return html.replace(
            Regex("""(?s)<pre><code>(.+?)<<<<<<< SEARCH\n(.+?)=======\n(.+?)>>>>>>> REPLACE\n</code></pre>"""),
            { matchResult ->
                val (_, filePath, searchBlock, replaceBlock) = matchResult.groupValues
                """
                <div class="file-path">$filePath</div>
                <pre>
                <code class="search-block">$searchBlock</code>
                <div class="divider"></div>
                <code class="replace-block">$replaceBlock</code>
                </pre>
                """.trimIndent()
            }
        )
    }
