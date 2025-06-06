package de.andrena.codingaider.outputview.markdown

import javax.swing.JComponent

interface MarkdownRenderer {
    val component: JComponent
    fun setMarkdown(markdown: String)
    fun setDarkTheme(isDarkTheme: Boolean)
    val isReady: Boolean
    fun scrollToBottom() {}
    fun dispose() {}
    fun supportsDevTools(): Boolean
    fun showDevTools(): Boolean
}
