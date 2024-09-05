package de.andrena.codingaider.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

class AiderTestCommand(private val project: Project, private val command: String) {
    fun execute() {
        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            
            AiderTestResultDialog(project, exitCode == 0, output.toString()).show()
        } catch (e: Exception) {
            AiderTestResultDialog(project, false, "Error executing Aider test: ${e.message}").show()
        }
    }
}

private class AiderTestResultDialog(
    project: Project,
    private val isSuccessful: Boolean,
    private val output: String
) : DialogWrapper(project) {

    init {
        title = if (isSuccessful) "Aider Test Successful" else "Aider Test Failed"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val textArea = JTextArea().apply {
            text = if (isSuccessful) {
                "Aider is installed and working correctly.\n\nOutput:\n$output"
            } else {
                "Aider test failed.\n\nOutput:\n$output"
            }
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scrollPane = JBScrollPane(textArea)
        scrollPane.preferredSize = Dimension(600, 400)
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }
}
