package de.andrena.codingaider.settings.tabs

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.LlmComboBoxRenderer
import de.andrena.codingaider.settings.LlmSelection
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

/**
 * General settings tab panel
 */
class GeneralSettingsTabPanel(
    apiKeyChecker: ApiKeyChecker = service<DefaultApiKeyChecker>()
) : SettingsTabPanel(apiKeyChecker) {

    // UI Components
    private val useYesFlagCheckBox = JBCheckBox("Use --yes flag by default")
    private var llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
    private val llmComboBox: JComboBox<LlmSelection> = createLlmComboBox(llmOptions)
    private val additionalArgsField = JBTextField()
    private val alwaysIncludeOpenFilesCheckBox = JBCheckBox("Always include open files in context")
    private val defaultModeComboBox = ComboBox(AiderMode.values())

    override fun getTabName(): String = "General"

    override fun getTabTooltip(): String = "Basic configuration options for Aider"

    override fun createPanel(panel: Panel) {
        panel.apply {
            group("Basic Settings") {
                row {
                    cell(useYesFlagCheckBox).applyToComponent {
                        toolTipText =
                            "When enabled, Aider will automatically accept changes without asking for confirmation"
                    }
                }
                row("Default Mode:") {
                    cell(defaultModeComboBox).component.apply {
                        toolTipText = "Select the default mode for Aider dialogs"
                    }
                }
                row {
                    cell(alwaysIncludeOpenFilesCheckBox).applyToComponent {
                        toolTipText = "When enabled, all currently open files will be included in the context"
                    }
                }
            }

            group("LLM Configuration") {
                row("Default LLM Model:") {
                    cell(llmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer(apiKeyChecker)
                        toolTipText = "Select the default LLM model to use for Aider operations"
                    }
                }
                row("Default Additional Arguments:") {
                    cell(additionalArgsField)
                        .resizableColumn()
                        .align(Align.FILL)
                        .applyToComponent {
                            toolTipText = "Additional command-line arguments to pass to Aider"
                        }
                    link("Aider options documentation") {
                        BrowserUtil.browse("https://aider.chat/docs/config/options.html")
                    }
                }
            }
        }
    }

    override fun apply() {
        settings.useYesFlag = useYesFlagCheckBox.isSelected
        settings.llm = llmComboBox.selectedItem.asSelectedItemName()
        settings.additionalArgs = additionalArgsField.text
        settings.alwaysIncludeOpenFiles = alwaysIncludeOpenFilesCheckBox.isSelected
        settings.defaultMode = defaultModeComboBox.selectedItem as AiderMode
    }

    override fun reset() {
        useYesFlagCheckBox.isSelected = settings.useYesFlag
        llmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.llm)
        additionalArgsField.text = settings.additionalArgs
        alwaysIncludeOpenFilesCheckBox.isSelected = settings.alwaysIncludeOpenFiles
        defaultModeComboBox.selectedItem = settings.defaultMode
    }

    override fun isModified(): Boolean {
        return useYesFlagCheckBox.isSelected != settings.useYesFlag ||
                llmComboBox.selectedItem.asSelectedItemName() != settings.llm ||
                additionalArgsField.text != settings.additionalArgs ||
                alwaysIncludeOpenFilesCheckBox.isSelected != settings.alwaysIncludeOpenFiles ||
                defaultModeComboBox.selectedItem != settings.defaultMode
    }

    fun updateLlmOptions() {
        llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
        val currentSelection = llmComboBox.selectedItem as? LlmSelection
        llmComboBox.model = DefaultComboBoxModel(llmOptions)
        if (currentSelection != null && llmOptions.contains(currentSelection)) {
            llmComboBox.selectedItem = currentSelection
        }
    }

    private fun createLlmComboBox(llmOptions: Array<LlmSelection>): JComboBox<LlmSelection> =
        object : JComboBox<LlmSelection>(llmOptions) {
            override fun getToolTipText(): String? {
                val selectedItem = selectedItem as? LlmSelection ?: return null
                return if (apiKeyChecker.isApiKeyAvailableForLlm(selectedItem.name)) {
                    "API key found for ${selectedItem.name}"
                } else {
                    "API key not found for ${selectedItem.name}"
                }
            }
        }

    private fun Any?.asSelectedItemName(): String {
        val selection = this as? LlmSelection ?: return ""
        return selection.name.ifBlank { "" }
    }
}
