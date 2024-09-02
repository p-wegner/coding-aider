package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.awt.BorderLayout
import javax.swing.JDialog
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

class ProgressDialog(project: Project, title: String) {
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
        dialog.pack()
        dialog.setLocationRelativeTo(projectFrame)
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
            dialog.dispose()
        }
    }
}