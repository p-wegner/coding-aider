package de.andrena.codingaider.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.BufferedReader
import java.io.InputStreamReader

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
            if (exitCode == 0) {
                Messages.showInfoMessage(
                    project,
                    "Aider is installed and working correctly.\n\nOutput:\n$output",
                    "Aider Test Successful"
                )
            } else {
                Messages.showErrorDialog(
                    project,
                    "Aider test failed with exit code $exitCode.\n\nOutput:\n$output",
                    "Aider Test Failed"
                )
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Error executing Aider test: ${e.message}", "Aider Test Error")
        }
    }
}
