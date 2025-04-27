package de.andrena.codingaider.outputview

import com.intellij.ui.jcef.JBCefApp
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.tables.TablesExtension

/**
 * Contract that any markdown viewer implementation must fulfill.
 *
 * Having this interface allows the UI layer (e.g. [MarkdownDialog]) to be
 * completely agnostic about how markdown is rendered – JCEF, Swing, or any
 * future technology can be swapped in via [MarkdownViewerFactory].
 */
interface MarkdownViewer {
    /** Swing component to be embedded in dialogs or tool‑windows. */
    val component: JComponent

    /** Replace the currently displayed markdown. Implementations should efficiently
     * update their view and preserve scroll‑position where possible. */
    fun setMarkdown(markdown: String)

    /** Optional hook that can be used by the dialog after the component becomes
     * visible to force deferred rendering or scroll‑restoration. */
    fun ensureContentDisplayed() {}

    /** Theme toggle propagated from the IDE. */
    fun setDarkTheme(dark: Boolean) {}
}

/**
 * Creates the most appropriate viewer for the current runtime.
 * ‑ If JCEF is available we use the clean JCEF implementation.
 * ‑ Otherwise we use a lightweight Swing implementation so that the plugin
 *   still works in headless test environments or IDEs where JCEF is disabled.
 */
object MarkdownViewerFactory {
    @JvmStatic
    fun create(lookupPaths: List<String> = emptyList()): MarkdownViewer =
        if (JBCefApp.isSupported()) CleanMarkdownJcefViewer(lookupPaths)
        else SwingMarkdownViewer(lookupPaths)
}

/**
 * Minimal Swing fallback so that rendering still works without JCEF.
 * Only a subset of the fancy features are available, but the interface
 * contract is honoured so callers do not need to care.
 */
private class SwingMarkdownViewer(
    private val lookupPaths: List<String> = emptyList()
) : MarkdownViewer {

    private val editorPane = JEditorPane("text/html", "").apply {
        isEditable = false
    }

    override val component: JComponent = JScrollPane(editorPane)

    private val options = MutableDataSet().apply {
        set(
            Parser.EXTENSIONS, listOf(
                TablesExtension.create(),
                AutolinkExtension.create()
            )
        )
    }
    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    override fun setMarkdown(markdown: String) {
        val doc = parser.parse(markdown)
        editorPane.text = renderer.render(doc)
    }

    override fun setDarkTheme(dark: Boolean) {
        // Basic theme handling – adjust foreground/background
        editorPane.background = if (dark) java.awt.Color(0x2B2B2B) else java.awt.Color.WHITE
        editorPane.foreground = if (dark) java.awt.Color.WHITE else java.awt.Color.BLACK
    }
}
