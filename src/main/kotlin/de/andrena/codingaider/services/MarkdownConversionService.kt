package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

@Service(Service.Level.PROJECT)
class MarkdownConversionService(project: Project) {

    fun convertHtmlToMarkdown(htmlContent: String, baseUrl: String): String {
        // Clean HTML with jsoup first
        val cleanHtml = Jsoup.clean(
            htmlContent, baseUrl, Safelist.relaxed()
                .addTags("div", "span", "pre", "code")
                .addAttributes("pre", "class")
                .addAttributes("code", "class")
        )

        // Further process with jsoup to improve structure
        val doc = Jsoup.parse(cleanHtml)
        // Remove common noise elements
        doc.select("nav, footer, .sidebar, .advertisement, .banner, script, style, iframe").remove()

        // Configure flexmark with extensions
        val options = MutableDataSet().apply {
            set(
                Parser.EXTENSIONS, listOf(
                    TablesExtension.create(),
                    StrikethroughExtension.create(),
                    AutolinkExtension.create()
                )
            )
            // Optimize HTML to Markdown conversion
            set(HtmlRenderer.SOFT_BREAK, "\n")
            set(HtmlRenderer.GENERATE_HEADER_ID, true)
            set(Parser.LISTS_AUTO_LOOSE, false)
            set(Parser.HEADING_NO_ATX_SPACE, true)
        }

        val parser = Parser.builder(options).build()
        val renderer = HtmlRenderer.builder(options).build()
        val document = parser.parse(doc.html())
        return renderer.render(document)
            .replace(Regex("\\n{3,}"), "\n\n") // Remove excessive newlines
            .trim()
    }
}
