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
package de.andrena.aidershortcut.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(
    name = "de.andrena.aidershortcut.settings.AiderSettingsState",
    storages = [Storage("AiderSettings.xml")]
)
@Service(Service.Level.PROJECT)
class AiderSettingsState : PersistentStateComponent<AiderSettingsState.State> {
    data class State(
        var lastTestResult: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): AiderSettingsState =
            project.getService(AiderSettingsState::class.java)
    }
}
package de.andrena.aidershortcut.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.BufferedReader
import java.io.InputStreamReader

class AiderTestCommand(private val project: Project) {
    fun execute() {
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
