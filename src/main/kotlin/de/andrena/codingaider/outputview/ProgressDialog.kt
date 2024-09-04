package de.andrena.codingaider.outputview

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import javax.swing.*

class ProgressDialog(project: Project, title: String, parentComponent: Component?) {
    private val dialog: JDialog
    private val outputTextArea = JTextArea(20, 50).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val scrollPane = JScrollPane(outputTextArea)

    init {
        val projectFrame = WindowManager.getInstance().getFrame(project)
        dialog = JDialog(projectFrame, title, false) // false means non-modal
        dialog.contentPane.add(scrollPane, BorderLayout.CENTER)

        val closeButton = JButton("Close").apply {
            mnemonic = KeyEvent.VK_C
            addActionListener { dialog.dispose() }
        }
        dialog.contentPane.add(closeButton, BorderLayout.SOUTH)

        dialog.pack()
        dialog.setLocationRelativeTo(parentComponent ?: projectFrame)
        dialog.isVisible = true
    }

    fun updateProgress(output: String, message: String) {
        SwingUtilities.invokeLater {
            outputTextArea.text = output
            dialog.title = message
            outputTextArea.caretPosition = outputTextArea.document.length
        }
    }

    fun finish() {
        SwingUtilities.invokeLater {
            dialog.title = "Aider Command Completed"
        }
    }
}


