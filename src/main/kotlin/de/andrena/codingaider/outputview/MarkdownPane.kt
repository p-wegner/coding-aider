package de.andrena.codingaider.outputview

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider
import javax.swing.JPanel
import java.awt.BorderLayout
import com.intellij.openapi.project.Project

class MarkdownPane : JPanel(BorderLayout()) {
    private lateinit var htmlPanel: MarkdownHtmlPanel

    fun initialize(project: Project) {
        val provider = MarkdownHtmlPanelProvider.createFromInfo(
            MarkdownHtmlPanelProvider.getAvailableProviders().first().providerInfo
        )
        htmlPanel = provider.createHtmlPanel()
        add(JBScrollPane(htmlPanel.component), BorderLayout.CENTER)
    }

    fun setMarkdownText(text: String) {
        val isDarkTheme = EditorColorsManager.getInstance().isDarkEditor
        val backgroundColor = if (isDarkTheme) JBColor.background() else JBColor.WHITE
        htmlPanel.setHtml(
            """
            <html>
            <head>
                <style>
                    body { 
                        background-color: ${String.format("#%02x%02x%02x", backgroundColor.red, backgroundColor.green, backgroundColor.blue)};
                        color: ${if (isDarkTheme) "#FFFFFF" else "#000000"};
                        font-family: 'Segoe UI', Arial, sans-serif;
                        line-height: 1.6;
                        padding: 20px;
                    }
                    pre, code {
                        background-color: ${if (isDarkTheme) "#2b2b2b" else "#f0f0f0"};
                        border-radius: 3px;
                        font-family: 'Courier New', Courier, monospace;
                        padding: 2px 4px;
                    }
                    pre {
                        padding: 10px;
                        overflow-x: auto;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        margin-top: 24px;
                        margin-bottom: 16px;
                        font-weight: 600;
                        line-height: 1.25;
                    }
                    a {
                        color: ${if (isDarkTheme) "#79b8ff" else "#0366d6"};
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    blockquote {
                        padding: 0 1em;
                        color: ${if (isDarkTheme) "#9e9e9e" else "#6a737d"};
                        border-left: 0.25em solid ${if (isDarkTheme) "#4a4a4a" else "#dfe2e5"};
                        margin: 0;
                    }
                    table {
                        border-collapse: collapse;
                        margin: 15px 0;
                    }
                    table, th, td {
                        border: 1px solid ${if (isDarkTheme) "#4a4a4a" else "#dfe2e5"};
                        padding: 6px 13px;
                    }
                    img {
                        max-width: 100%;
                        box-sizing: border-box;
                    }
                </style>
            </head>
            <body>
            $text
            </body>
            </html>
            """.trimIndent(),
            0
        )
    }

    private var currentText = ""

    fun appendMarkdownText(text: String) {
        currentText += text
        setMarkdownText(currentText)
    }
}
