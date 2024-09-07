package de.andrena.codingaider.outputview

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.EventQueue.invokeLater
import java.awt.event.KeyEvent
import javax.swing.*
import kotlin.concurrent.schedule
import java.util.Timer
import java.util.TimerTask

class MarkdownDialog(private val project: Project, title: String, initialText: String) : JDialog() {
    private val textArea: JTextArea = JTextArea(initialText)
    private val scrollPane: JScrollPane
    private var autoCloseTimer: TimerTask? = null
    private val keepOpenButton: JButton

    init {
        this.title = title
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
    }

    fun updateProgress(output: String, message: String) {
        invokeLater {
            textArea.text = output
            this.title = message
            textArea.caretPosition = textArea.document.length
            scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
        }
    }

    fun startAutoCloseTimer() {
        keepOpenButton.isVisible = true
        autoCloseTimer = Timer().schedule(30000) { // 30 seconds
            invokeLater { dispose() }
        }
    }

    private fun cancelAutoClose() {
        autoCloseTimer?.cancel()
        keepOpenButton.isVisible = false
    }
}
