package de.andrena.codingaider.outputview

import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.ui.preview.MarkdownEditorWithPreview
import java.awt.BorderLayout
import java.awt.EventQueue.invokeLater
import java.awt.Frame
import java.awt.event.KeyEvent
import java.util.*
import java.util.Timer
import javax.swing.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.max

class MarkdownDialog(
    private val project: Project,
    private val initialTitle: String,
    initialText: String,
    private val onAbort: Abortable? = null
) : JDialog(null as Frame?, true) {
    // use MarkdownEditorWithPreview instead of LanguageTextField to enable preview

    private val textArea: MarkdownEditorWithPreview = run {
        val virtualFile = LightVirtualFile("preview.md", MarkdownFileType.INSTANCE, initialText.replace("\r\n", "\n"))
        MarkdownEditorWithPreview(project, virtualFile).apply {
            editor.settings.apply {
                isUseSoftWraps = true
                isAdditionalPageAtBottom = true
            }
        }
    }
    private lateinit var scrollPane: JScrollPane
    private var autoCloseTimer: TimerTask? = null
    private lateinit var keepOpenButton: JButton
    private lateinit var closeButton: JButton
    private var isProcessFinished = false

    init {
        title = initialTitle
        setSize(800, 800)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        scrollPane = JBScrollPane(textArea.component).apply {
            setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
            setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
            (getViewport() as JViewport).scrollMode = JViewport.BACKINGSTORE_SCROLL_MODE
        }
        add(scrollPane, BorderLayout.CENTER)

        val buttonPanel = JPanel()
        closeButton = JButton(onAbort?.let { "Abort" } ?: "Close").apply {
            mnemonic = onAbort?.let { KeyEvent.VK_A } ?: KeyEvent.VK_C
            addActionListener {
                if (isProcessFinished || onAbort == null) {
                    dispose()
                } else {
                    onAbort.abortCommand()
                }
            }
        }
        keepOpenButton = JButton("Keep Open").apply {
            mnemonic = KeyEvent.VK_K
            addActionListener { cancelAutoClose() }
            isVisible = false
        }
        buttonPanel.add(closeButton)
        buttonPanel.add(keepOpenButton)
        add(buttonPanel, BorderLayout.SOUTH)

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(windowEvent: java.awt.event.WindowEvent?) {
                if (isProcessFinished || onAbort == null) {
                    dispose()
                } else {
                    onAbort.abortCommand()
                }
            }
        })
        isVisible = true
        setAlwaysOnTop(true)
        setAlwaysOnTop(false)
    }

    fun updateProgress(output: String, message: String) {
        invokeLater {
            textArea.editor.document.setText(output)
            title = message
//            textArea.caretPosition = textArea.document.length
            scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
        }
    }

    fun startAutoCloseTimer(autocloseDelay: Int) {
        val settings = getInstance()
        if (!settings.enableMarkdownDialogAutoclose) return
        keepOpenButton.isVisible = true
        var remainingSeconds = max(1, autocloseDelay)
        autoCloseTimer = Timer().scheduleAtFixedRate(0, 1000) { // Update every second
            invokeLater {
                if (remainingSeconds > 0) {
                    title = "$initialTitle - Closing in $remainingSeconds seconds"
                    remainingSeconds--
                } else {
                    dispose()
                }
            }
        }
    }

    private fun cancelAutoClose() {
        autoCloseTimer?.cancel()
        keepOpenButton.isVisible = false
        title = initialTitle
    }

    fun setProcessFinished() {
        isProcessFinished = true
        invokeLater {
            closeButton.text = "Close"
            closeButton.mnemonic = KeyEvent.VK_C
        }
    }

    fun focus(delay: Long = 100) {
        Timer().schedule(delay) {
            SwingUtilities.invokeLater {
                toFront()
                requestFocus()
                isAlwaysOnTop = true
                isAlwaysOnTop = false
                textArea.component.requestFocusInWindow()
            }
        }
    }

}
