package de.andrena.codingaider.outputview.markdown

import javax.swing.JComponent

interface MarkdownRenderer {
    val component: JComponent
    fun setMarkdown(markdown: String)
    fun setDarkTheme(isDarkTheme: Boolean)
    val isReady: Boolean
    fun showDevTools(): Boolean = false
    fun supportsDevTools(): Boolean = false
    fun scrollToBottom() {}
    fun dispose() {}
}
