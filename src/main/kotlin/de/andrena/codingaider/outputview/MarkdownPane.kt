package de.andrena.codingaider.outputview

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanelProvider
import javax.swing.JPanel
import java.awt.BorderLayout

class MarkdownPane : JPanel(BorderLayout()) {
    private val htmlPanel: MarkdownHtmlPanel

    init {
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

}
