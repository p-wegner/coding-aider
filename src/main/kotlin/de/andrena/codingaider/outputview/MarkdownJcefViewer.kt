package de.andrena.codingaider.outputview

import com.intellij.ui.JBColor
import de.andrena.codingaider.outputview.markdown.MarkdownViewer
import javax.swing.JComponent

/**
 * A simplified Markdown viewer component that uses JCEF (Chromium Embedded Framework)
 * with fallback to JEditorPane when JCEF is not available.
 * 
 * This class is a wrapper around the refactored MarkdownViewer implementation
 * to maintain backward compatibility with existing code.
 */
class MarkdownJcefViewer(private val lookupPaths: List<String> = emptyList()) {

    private val markdownViewer = MarkdownViewer(lookupPaths)

    init {
        // Initialize with current theme
        markdownViewer.setDarkTheme(!JBColor.isBright())
    }

    val component: JComponent
        get() = markdownViewer.component

    /**
     * Sets the markdown content to be displayed
     */
    fun setMarkdown(markdown: String) {
        markdownViewer.setMarkdown(markdown)
    }

    /**
     * Updates the theme used for rendering
     */
    fun setDarkTheme(dark: Boolean) {
        markdownViewer.setDarkTheme(dark)
    }
}
