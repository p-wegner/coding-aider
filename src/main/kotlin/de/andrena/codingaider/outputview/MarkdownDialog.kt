package de.andrena.codingaider.outputview

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.EventQueue.invokeLater
import java.awt.event.KeyEvent
import java.util.*
import java.util.Timer
import javax.swing.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate

class MarkdownDialog(private val project: Project, private val initialTitle: String, initialText: String) : JDialog() {
    private val textArea: JTextArea = JTextArea(initialText)
    private val scrollPane: JScrollPane
    private var autoCloseTimer: TimerTask? = null
    private val keepOpenButton: JButton

    init {
        title = initialTitle
        setSize(800, 800)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        textArea.isEditable = false
        scrollPane = JBScrollPane(textArea)
        add(scrollPane, BorderLayout.CENTER)

        val buttonPanel = JPanel()
        val closeButton = JButton("Close").apply {
            mnemonic = KeyEvent.VK_C
            addActionListener { dispose() }
        }
        keepOpenButton = JButton("Keep Open").apply {
            mnemonic = KeyEvent.VK_K
            addActionListener { cancelAutoClose() }
            isVisible = false
        }
        buttonPanel.add(closeButton)
        buttonPanel.add(keepOpenButton)
        add(buttonPanel, BorderLayout.SOUTH)

        defaultCloseOperation = DISPOSE_ON_CLOSE
        isVisible = true
        setAlwaysOnTop(true)
        setAlwaysOnTop(false)
    }

    fun updateProgress(output: String, message: String) {
        invokeLater {
            textArea.text = output
            title = message
            textArea.caretPosition = textArea.document.length
            scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
        }
    }

    fun startAutoCloseTimer() {
        keepOpenButton.isVisible = true
        var remainingSeconds = 10
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

    fun focus(delay: Long = 100) {
        Timer().schedule(delay) {
            SwingUtilities.invokeLater {
                toFront()
                requestFocus()
                isAlwaysOnTop = true
                isAlwaysOnTop = false
                textArea.requestFocusInWindow()
            }
        }
    }
}
