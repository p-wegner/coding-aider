package de.andrena.aidershortcut.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

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
                    AiderTestCommand(project, "aider --help").execute()
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



