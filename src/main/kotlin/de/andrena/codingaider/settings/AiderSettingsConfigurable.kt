package de.andrena.codingaider.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class AiderSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private val llmOptions = arrayOf("--sonnet", "--mini", "--4o", "--deepseek", "")
    private val llmComboBox = com.intellij.openapi.ui.ComboBox(llmOptions)
    private val additionalArgsField = JBTextField()
    private val isShellModeCheckBox = JBCheckBox("Use Shell Mode by default")

    override fun getDisplayName(): String = "Aider"

    override fun createComponent(): JComponent {
        settingsComponent = panel {
            group("General Settings") {
                row {
                    cell(useYesFlagCheckBox)
                }
                row("Default LLM Model:") {
                    cell(llmComboBox)
                }
                row("Default Additional Arguments:") {
                    cell(additionalArgsField)
                        .resizableColumn()
                        .align(Align.FILL)
                }
                row {
                    cell(isShellModeCheckBox)
                }
            }

            group("Installation") {
                row {
                    button("Test Aider Installation") {
                        AiderTestCommand(project, "aider --help").execute()
                    }
                }
            }
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


