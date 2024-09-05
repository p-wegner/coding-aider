package de.andrena.codingaider.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import javax.swing.*
import java.awt.Component

class AiderSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private val llmOptions = arrayOf("--sonnet", "--mini", "--4o", "--deepseek", "")
    private val llmComboBox = object : JComboBox<String>(llmOptions) {
        override fun getToolTipText(): String? {
            val selectedItem = selectedItem as? String ?: return null
            val apiKey = when {
                selectedItem.contains("sonnet") -> "ANTHROPIC_API_KEY"
                selectedItem.contains("mini") || selectedItem.contains("4o") -> "OPENAI_API_KEY"
                selectedItem.contains("deepseek") -> "DEEPSEEK_API_KEY"
                else -> return null
            }
            return if (ApiKeyChecker.checkApiKeys()[apiKey] == true) {
                "API key found for $selectedItem"
            } else {
                "API key not found for $selectedItem"
            }
        }
    }
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
                        .component.apply {
                            renderer = LlmComboBoxRenderer()
                        }
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

    private inner class LlmComboBoxRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is String) {
                val apiKey = when {
                    value.contains("sonnet") -> "ANTHROPIC_API_KEY"
                    value.contains("mini") || value.contains("4o") -> "OPENAI_API_KEY"
                    value.contains("deepseek") -> "DEEPSEEK_API_KEY"
                    else -> null
                }
                if (apiKey != null) {
                    val isKeyAvailable = ApiKeyChecker.checkApiKeys()[apiKey] == true
                    icon = if (isKeyAvailable) {
                        UIManager.getIcon("OptionPane.informationIcon")
                    } else {
                        UIManager.getIcon("OptionPane.errorIcon")
                    }
                    toolTipText = if (isKeyAvailable) {
                        "API key found for $value"
                    } else {
                        "API key not found for $value"
                    }
                } else {
                    icon = null
                    toolTipText = null
                }
            }
            return component
        }
    }
}


