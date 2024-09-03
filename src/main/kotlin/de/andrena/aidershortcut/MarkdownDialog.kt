package de.andrena.aidershortcut

import javax.swing.JDialog
import javax.swing.JTextArea
import javax.swing.JScrollPane
import javax.swing.JButton
import java.awt.BorderLayout
import java.awt.event.ActionListener

class MarkdownDialog(private val project: Project, title: String, initialText: String) : JDialog() {
    private val textArea: JTextArea = JTextArea(initialText)

    init {
        this.title = title
        setSize(400, 300)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        val scrollPane = JScrollPane(textArea)
        add(scrollPane, BorderLayout.CENTER)

        val button = JButton("Close")
        button.addActionListener { dispose() }
        add(button, BorderLayout.SOUTH)
    }

    fun isCaretVisible(): Boolean {
        return textArea.caret.isVisible
    }

    fun updateProgress(output: String, message: String) {
        textArea.text = output
        title = message
    }

    fun finish() {
        dispose()
    }
}
