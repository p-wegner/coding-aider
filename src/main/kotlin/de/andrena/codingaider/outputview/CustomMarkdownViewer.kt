package de.andrena.codingaider.outputview

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.vfs.LocalFileSystem
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.html.MutableAttributes
import com.vladsch.flexmark.html.AttributeProviderFactory
import com.vladsch.flexmark.html.renderer.AttributablePart
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem
import com.vladsch.flexmark.html.AttributeProvider
import com.vladsch.flexmark.html.renderer.LinkResolverContext
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent

class CustomMarkdownViewer {
    val component: JEditorPane = JEditorPane().apply {
        contentType = "text/html"
        isEditable = false
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        putClientProperty("JEditorPane.honorDisplayProperties", true)
    }
    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            TaskListExtension.create(),
            DefinitionExtension.create(),
            FootnoteExtension.create(),
            TocExtension.create()
        ))
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

    init {
        component.addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    val url = event.url?.toString() ?: event.description
                    val project = com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
                    
                    val file = when {
                        url.startsWith("file:") -> {
                            // Handle absolute file paths
                            val filePath = java.net.URLDecoder.decode(url.removePrefix("file:"), "UTF-8")
                            File(filePath)
                        }
                        project != null && !url.contains("://") -> {
                            // Handle relative paths within project
                            val basePath = project.basePath
                            if (basePath != null) {
                                File(basePath, url)
                            } else {
                                throw IllegalArgumentException("Project base path not found")
                            }
                        }
                        url.startsWith("./") -> {
                            // Handle relative paths by checking multiple lookup locations
                            val basePath = project?.basePath
                            if (basePath != null) {
                                val relativePath = url.removePrefix("./")
                                // First try project root
                                var file = File(basePath, relativePath)
                                if (file.exists()) {
                                    file
                                } else {
                                    // Then try each lookup path
                                    LOOKUP_PATHS.map { lookupPath -> 
                                        File(basePath, "$lookupPath/$relativePath")
                                    }.firstOrNull { it.exists() }
                                        ?: throw IllegalArgumentException("File not found in any lookup path: $relativePath")
                                }
                            } else {
                                throw IllegalArgumentException("Project base path not found")
                            }
                        }
                        else -> null
                    }

                    if (file != null && project != null) {
                        
                        // Open file in IDE
                        OpenFileDescriptor(
                            project,
                            LocalFileSystem.getInstance().findFileByIoFile(file)
                                ?: throw IllegalArgumentException("File not found: ${file.path}")
                        ).navigate(true)
                    } else {
                        // For external URLs or when file/project not found, use default browser
                        Desktop.getDesktop().browse(URI(url))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private var currentContent = ""
    private var isDarkTheme = false

    companion object {
        private val LOOKUP_PATHS = listOf(
            ".coding-aider-plans"  // Default plans folder
        )

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
        ): String = """
            <html>
            <head>
                <style type="text/css">
                    body { 
                        font-family: Arial, sans-serif; 
                        margin: 20px;
                        line-height: 1.6;
                        background-color: $bodyBg;
                        color: $bodyText;
                    }
                    pre { 
                        background-color: $preBg; 
                        padding: 10px; 
                        border: 1px solid $preBorder;
                        color: $preText;
                    }
                    code { 
                        font-family: "JetBrains Mono", "Courier New", Courier, monospace;
                        color: $codeColor;
                    }
                    a { 
                        color: $linkColor; 
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
                        border: 1px solid $tableBorder;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: $thBg;
                    }
                    tr:nth-child(even) {
                        background-color: $trEvenBg;
                    }
                    /* Headings */
                    h1, h2, h3, h4, h5, h6 {
                        margin-top: 24px;
                        margin-bottom: 16px;
                        font-weight: bold;
                        line-height: 1.25;
                    }
                    h1 { font-size: 2em; border-bottom: 1px solid $tableBorder; }
                    h2 { font-size: 1.5em; border-bottom: 1px solid $tableBorder; }
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
                        border-top: 1px solid $tableBorder;
                        margin-top: 2em;
                        padding-top: 1em;
                    }
                </style>
            </head>
            <body>
                $content
            </body>
            </html>
        """
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
            val document = parser.parse(markdown)
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
}


