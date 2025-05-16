package de.andrena.codingaider.outputview.markdown

import javax.swing.JComponent

interface MarkdownRenderer {
    val component: JComponent
    fun setMarkdown(markdown: String)
    fun setDarkTheme(isDarkTheme: Boolean)
    val isReady: Boolean
    fun showDevTools(): Boolean = false
    fun supportsDevTools(): Boolean = false
    
    /**
     * Scrolls to the bottom of the content if supported by the renderer
     */
    fun scrollToBottom() {}
    
    /**
     * Releases resources used by the renderer
     */
    fun dispose() {}
}
