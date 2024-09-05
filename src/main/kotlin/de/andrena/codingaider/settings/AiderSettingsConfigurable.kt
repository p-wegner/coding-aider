package de.andrena.codingaider.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class AiderSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private val llmOptions = arrayOf("--sonnet", "--mini", "--4o", "--deepseek", "")
    private val llmComboBox = JComboBox(llmOptions)
    private val additionalArgsField = JBTextField()
    private val isShellModeCheckBox = JBCheckBox("Use Shell Mode by default")

    override fun getDisplayName(): String = "Aider Settings"

    override fun createComponent(): JComponent {
        val formBuilder = FormBuilder.createFormBuilder()
            .addComponent(useYesFlagCheckBox)
            .addLabeledComponent("Default LLM Model:", llmComboBox)
            .addLabeledComponent("Default Additional Arguments:", additionalArgsField)
            .addComponent(isShellModeCheckBox)

        settingsComponent = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                fill = GridBagConstraints.HORIZONTAL
                insets = JBUI.insets(5)
            }

            add(formBuilder.panel, gbc)

            gbc.gridy++
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

    override fun isModified(): Boolean {
        val settings = AiderSettings.getInstance(project)
        return useYesFlagCheckBox.isSelected != settings.useYesFlag ||
                llmComboBox.selectedItem as String != settings.llm ||
                additionalArgsField.text != settings.additionalArgs ||
                isShellModeCheckBox.isSelected != settings.isShellMode
    }

    override fun apply() {
        val settings = AiderSettings.getInstance(project)
        settings.useYesFlag = useYesFlagCheckBox.isSelected
        settings.llm = llmComboBox.selectedItem as String
        settings.additionalArgs = additionalArgsField.text
        settings.isShellMode = isShellModeCheckBox.isSelected
    }

    override fun reset() {
        val settings = AiderSettings.getInstance(project)
        useYesFlagCheckBox.isSelected = settings.useYesFlag
        llmComboBox.selectedItem = settings.llm
        additionalArgsField.text = settings.additionalArgs
        isShellModeCheckBox.isSelected = settings.isShellMode
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}


