package de.andrena.codingaider.outputview.markdown

import com.intellij.openapi.project.ProjectManager
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.definition.DefinitionExtension
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.toc.TocExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import de.andrena.codingaider.utils.FilePathConverter

/**
 * Processes markdown content and converts it to HTML with special handling for
 * code blocks, search/replace blocks, and other custom elements.
 */
class MarkdownContentProcessor(private val lookupPaths: List<String> = emptyList()) {

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

    private val parser = Parser.builder(markdownOptions).build()
    private val renderer = HtmlRenderer.builder(markdownOptions).build()

    /**
     * Converts markdown to HTML with all special processing applied
     */
    fun processMarkdown(markdown: String, isDarkTheme: Boolean): String {
        // Get project base path for file path conversion
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        val basePath = project?.basePath

        // Convert file paths to markdown links
        val processedMarkdown = FilePathConverter.convertPathsToMarkdownLinks(markdown, basePath)
        
        // Parse and render basic markdown
        val document = parser.parse(processedMarkdown)
        var html = renderer.render(document)

        // Process special aider blocks
        html = processAiderBlocks(html)

        // Apply styling and process search/replace blocks
        return applyStylesToHtml(html, isDarkTheme)
    }

    /**
     * Processes special aider blocks in the HTML
     */
    private fun processAiderBlocks(html: String): String {
        return html.replace(
            Regex("(?s)<aider-intention>\\s*(.*?)\\s*</aider-intention>")
        ) { matchResult ->
            val intentionContent = matchResult.groupValues[1].trim()
            val renderedContent = renderer.render(parser.parse(intentionContent))
            "<div class=\"aider-intention\">$renderedContent</div>"
        }.replace(
            Regex("(?s)<aider-summary>\\s*(.*?)\\s*</aider-summary>")
        ) { matchResult ->
            val summaryContent = matchResult.groupValues[1].trim()
            val renderedContent = renderer.render(parser.parse(summaryContent))
            "<div class=\"aider-summary\">$renderedContent</div>"
        }
    }

    /**
     * Applies CSS styles to the HTML based on the current theme
     */
    private fun applyStylesToHtml(html: String, isDarkTheme: Boolean): String {
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
            
            /* Custom scrollbar styling to match IDE native look */
            ::-webkit-scrollbar {
                width: 12px;
                height: 12px;
            }
            
            ::-webkit-scrollbar-track {
                background: ${if (isDarkTheme) "#3c3f41" else "#f0f0f0"};
                border-radius: 0;
            }
            
            ::-webkit-scrollbar-thumb {
                background: ${if (isDarkTheme) "#5a5d5e" else "#c9c9c9"};
                border-radius: 0;
                border: 2px solid ${if (isDarkTheme) "#3c3f41" else "#f0f0f0"};
            }
            
            ::-webkit-scrollbar-thumb:hover {
                background: ${if (isDarkTheme) "#6e7071" else "#a0a0a0"};
            }
            
            ::-webkit-scrollbar-corner {
                background: ${if (isDarkTheme) "#3c3f41" else "#f0f0f0"};
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
                transition: background-color 0.2s;
            }
            
            /* Disable hover effects during content updates */
            body.updating-content .collapsible-header:hover {
                background: ${colors["preBg"]} !important;
                transition: none !important;
            }
            
            /* Only apply hover effects when not updating */
            body:not(.updating-content) .collapsible-header:hover {
                background: ${if (isDarkTheme) "#383838" else "#e8e8e8"};
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
        
        ${processSearchReplaceBlocks(html)}
        """.trimIndent()

        return styledHtml
    }

    /**
     * Escapes HTML special characters in text
     */
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
        fun createCollapsiblePanel(
            title: String,
            content: String,
            cssClass: String = "",
            isEscaped: Boolean = true
        ): String {
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
            Regex("""<aider-command>\s*(.*?)\s*</aider-command>""", RegexOption.DOT_MATCHES_ALL) to
                    { content: String -> createCollapsiblePanel("Aider Command", content) },

            Regex("""<aider-system-prompt>(.*?)</aider-system-prompt>""", RegexOption.DOT_MATCHES_ALL) to
                    { content: String -> createCollapsiblePanel("System Prompt", content, "system") },

            Regex("""<aider-user-prompt>(.*?)</aider-user-prompt>""", RegexOption.DOT_MATCHES_ALL) to
                    { content: String -> createCollapsiblePanel("User Request", content, "user") },

            Regex("""<div class="aider-intention">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL) to
                    { content: String -> createCollapsiblePanel("Intention", content, "intention", isEscaped = false) },

            Regex("""<div class="aider-summary">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL) to
                    { content: String -> createCollapsiblePanel("Summary", content, "summary", isEscaped = false) }
        )

        // Apply block patterns
        blockPatterns.forEach { (pattern, formatter) ->
            processedHtml = processedHtml.replace(pattern) { matchResult ->
                formatter(matchResult.groupValues[1])
            }
        }

        // Process search/replace blocks - improved to better handle edit format blocks
        val searchReplacePattern = Regex(
            """(?m)^([^\n]+?)\n```[^\n]*\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        processedHtml = searchReplacePattern.replace(processedHtml) { matchResult ->
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
