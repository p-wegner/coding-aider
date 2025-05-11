package de.andrena.codingaider.outputview.markdown

import javax.swing.JComponent

/**
 * Interface for markdown rendering implementations.
 * Provides a common API for different rendering backends (JCEF, JEditorPane, etc.)
 */
interface MarkdownRenderer {
    /**
     * The UI component that displays the rendered markdown
     */
    val component: JComponent

    /**
     * Updates the rendered content with new markdown
     * @param markdown The markdown content to render
     */
    fun setMarkdown(markdown: String)

    /**
     * Updates the theme used for rendering
     * @param isDarkTheme True if dark theme should be used, false for light theme
     */
    fun setDarkTheme(isDarkTheme: Boolean)

    /**
     * Indicates if the renderer is ready to display content
     */
    val isReady: Boolean
    
    /**
     * Releases resources used by the renderer
     * Should be called when the component is no longer needed
     */
    fun dispose() {}
}
