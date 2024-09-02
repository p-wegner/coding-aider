package de.andrena.aidershortcut.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.JLabel
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets

class AiderSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null

    override fun getDisplayName(): String = "Aider Settings"

    override fun createComponent(): JComponent {
        settingsComponent = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(5, 5, 5, 5)
            }

            add(JLabel("Test Aider Installation:"), gbc)

            gbc.gridx = 1
            val testButton = JButton("Run Test").apply {
                addActionListener {
                    AiderTestCommand(project).execute()
                }
            }
            add(testButton, gbc)
        }
        return settingsComponent!!
    }

    override fun isModified(): Boolean = false

    override fun apply() {}

    override fun reset() {}

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
        try {
            val process = ProcessBuilder("aider", "--help").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Messages.showInfoMessage(project, "Aider is correctly installed and accessible.", "Aider Test Result")
            } else {
                Messages.showErrorDialog(project, "Aider test failed. Exit code: $exitCode", "Aider Test Result")
            }
            
            // Save the test result
            val settingsState = AiderSettingsState.getInstance(project)
            settingsState.loadState(AiderSettingsState.State(lastTestResult = output.toString()))
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Error executing Aider test: ${e.message}", "Aider Test Error")
        }
    }
}
