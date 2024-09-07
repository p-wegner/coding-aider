package de.andrena.codingaider.outputview

import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.EventQueue.invokeLater
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JScrollPane
import javax.swing.JTextArea

class MarkdownDialog(private val project: Project, title: String, initialText: String) : JDialog() {
    private val textArea: JTextArea = JTextArea(initialText)
    private val scrollPane: JScrollPane

    init {
        this.title = title
        setSize(800, 800)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        textArea.isEditable = false
        scrollPane = JScrollPane(textArea)
        add(scrollPane, BorderLayout.CENTER)

        val button = JButton("Close").apply {
            mnemonic = KeyEvent.VK_C
            addActionListener { dispose() }
        }
        add(button, BorderLayout.SOUTH)

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
}
