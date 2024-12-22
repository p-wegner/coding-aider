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

            val styledHtml = """
                <html>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; 
                            margin: 20px; line-height: 1.6; background-color: ${colors[0]}; color: ${colors[1]};">
                    $html
                </body>
                </html>
            """.trimIndent()

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


