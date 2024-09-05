package de.andrena.codingaider.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.bindText
import de.andrena.codingaider.utils.ApiKeyChecker
import com.intellij.ide.BrowserUtil
import java.awt.Component
import javax.swing.*

class AiderSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private val llmOptions = ApiKeyChecker.getAllLlmOptions().toTypedArray()
    private val llmComboBox = object : JComboBox<String>(llmOptions) {
        override fun getToolTipText(): String? {
            val selectedItem = selectedItem as? String ?: return null
            return if (ApiKeyChecker.isApiKeyAvailableForLlm(selectedItem)) {
                "API key found for $selectedItem"
            } else {
                "API key not found for $selectedItem"
            }
        }
    }
    private val additionalArgsField = JBTextField()
    private val isShellModeCheckBox = JBCheckBox("Use Shell Mode by default")
    private val lintCmdField = JBTextField()

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
                    link("Aider Options Documentation") {
                        BrowserUtil.browse("https://aider.chat/docs/config/options.html")
                    }
                }
                row {
                    cell(isShellModeCheckBox)
                }
                row("Lint Command:") {
                    cell(lintCmdField)
                        .resizableColumn()
                        .align(Align.FILL)
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
                isShellModeCheckBox.isSelected != settings.isShellMode ||
                lintCmdField.text != settings.lintCmd
    }

    override fun apply() {
        val settings = AiderSettings.getInstance(project)
        settings.useYesFlag = useYesFlagCheckBox.isSelected
        settings.llm = llmComboBox.selectedItem as String
        settings.additionalArgs = additionalArgsField.text
        settings.isShellMode = isShellModeCheckBox.isSelected
        settings.lintCmd = lintCmdField.text
    }

    override fun reset() {
        val settings = AiderSettings.getInstance(project)
        useYesFlagCheckBox.isSelected = settings.useYesFlag
        llmComboBox.selectedItem = settings.llm
        additionalArgsField.text = settings.additionalArgs
        isShellModeCheckBox.isSelected = settings.isShellMode
        lintCmdField.text = settings.lintCmd
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
                val apiKey = ApiKeyChecker.getApiKeyForLlm(value)
                if (apiKey != null && !ApiKeyChecker.isApiKeyAvailableForLlm(value)) {
                    icon = UIManager.getIcon("OptionPane.errorIcon")
                    toolTipText = "API key not found in default locations for $value. This may not be an error if you're using an alternative method to provide the key."
                } else {
                    icon = null
                    toolTipText = null
                }
            }
            return component
        }
    }
}


