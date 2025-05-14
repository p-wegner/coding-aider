package de.andrena.codingaider.outputview.markdown

import javax.swing.JComponent

interface MarkdownRenderer {
    val component: JComponent
    fun setMarkdown(markdown: String)
    fun setDarkTheme(isDarkTheme: Boolean)
    val isReady: Boolean
    
    /**
     * Shows developer tools if supported by the renderer
     * @return true if developer tools were shown, false otherwise
     */
    fun showDevTools(): Boolean = false
    
    fun dispose() {}
}
