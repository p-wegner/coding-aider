package de.andrena.codingaider.settings.tabs

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import de.andrena.codingaider.services.AiderEditFormat
import de.andrena.codingaider.settings.ExperimentalFeatureUtil
import de.andrena.codingaider.utils.ApiKeyChecker
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel

class CodeModificationTabPanel(apiKeyChecker: ApiKeyChecker) : SettingsTabPanel(apiKeyChecker) {

    // UI Components
    private val lintCmdField = JBTextField()
    private val editFormatComboBox = ComboBox(createEditFormatModel())
    private val promptAugmentationCheckBox = JBCheckBox("Enable prompt augmentation")
    private val includeCommitMessageBlockCheckBox = JBCheckBox("Include commit message block")
    private val reasoningEffortComboBox = ComboBox(arrayOf("", "low", "medium", "high"))
    
    // Plugin-based edits settings
    private val pluginBasedEditsCheckBox = JBCheckBox("Use Plugin-Based edits (Experimental)")
    private val lenientEditsCheckBox = JBCheckBox("Allow Lenient Edits (Process multiple formats) (Experimental)")
    private val autoCommitAfterEditsCheckBox = JBCheckBox("Auto-commit after plugin-based edits (Experimental)")

    override fun getTabName(): String = "Code Modification"

    override fun getTabTooltip(): String = "Settings for code editing and modification"

    override fun createPanel(panel: Panel) {
        panel.apply {
            group("Code Editing") {
                row("Lint Command:") {
                    cell(lintCmdField)
                        .resizableColumn()
                        .align(Align.FILL)
                        .apply {
                            component.toolTipText = "The lint command will be executed after every code change by Aider"
                        }
                }
                row("Edit Format:") {
                    cell(editFormatComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Select the default edit format for Aider. Leave empty to use the default format for the used LLM."
                        }
                }
                row("Reasoning Effort:") {
                    cell(reasoningEffortComboBox)
                        .component
                        .apply {
                            toolTipText = "Set the default reasoning effort level for the LLM"
                        }
                }
            }

            group("Prompt Augmentation") {
                row {
                    cell(promptAugmentationCheckBox)
                        .applyToComponent {
                            toolTipText =
                                "When enabled, Aider will include XML-tagged blocks in the prompt to structure the output"
                            addItemListener { e ->
                                val isSelected = e.stateChange == ItemEvent.SELECTED
                                includeCommitMessageBlockCheckBox.isEnabled = isSelected

                                // If prompt augmentation is disabled but auto-commit is enabled, show warning
                                if (!isSelected && autoCommitAfterEditsCheckBox.isSelected && pluginBasedEditsCheckBox.isSelected) {
                                    showNotification(
                                        "Warning: Auto-commit requires prompt augmentation with commit message block",
                                        NotificationType.WARNING
                                    )
                                    // Disable auto-commit if prompt augmentation is disabled
                                    autoCommitAfterEditsCheckBox.isSelected = false
                                }
                            }
                        }
                }
                row {
                    cell(includeCommitMessageBlockCheckBox)
                        .applyToComponent {
                            toolTipText =
                                "When enabled, Aider will include an XML block for commit messages in the prompt"
                            isEnabled = promptAugmentationCheckBox.isSelected
                            addItemListener { e ->
                                val isSelected = e.stateChange == ItemEvent.SELECTED

                                // If commit message block is disabled but auto-commit is enabled, show warning
                                if (!isSelected && autoCommitAfterEditsCheckBox.isSelected && pluginBasedEditsCheckBox.isSelected) {
                                    showNotification(
                                        "Warning: Auto-commit requires prompt augmentation with commit message block",
                                        NotificationType.WARNING
                                    )
                                    // Disable auto-commit if commit message block is disabled
                                    autoCommitAfterEditsCheckBox.isSelected = false
                                }
                            }
                        }
                }
            }

            group("Plugin-Based Edits") {
                row {
                    cell(pluginBasedEditsCheckBox)
                        .component
                        .apply {
                            text = "Use Plugin-Based Edits"
                            ExperimentalFeatureUtil.markAsExperimental(this)
                            toolTipText =
                                "If enabled, the plugin handles applying edits using /ask and a specific diff format, bypassing Aider's internal edit formats."
                            addItemListener { e ->
                                val isSelected = e.stateChange == ItemEvent.SELECTED
                                autoCommitAfterEditsCheckBox.isEnabled = isSelected
                                lenientEditsCheckBox.isEnabled = isSelected

                                // Update commit message block checkbox state based on auto-commit
                                if (isSelected && autoCommitAfterEditsCheckBox.isSelected) {
                                    promptAugmentationCheckBox.isSelected = true
                                    includeCommitMessageBlockCheckBox.isSelected = true
                                }
                            }
                        }
                }
                row {
                    cell(lenientEditsCheckBox)
                        .component
                        .apply {
                            text = "Allow Lenient Edits"
                            ExperimentalFeatureUtil.markAsExperimental(this)
                            toolTipText =
                                "If enabled, the plugin will process all edit formats (diff, whole, udiff) in a single response, regardless of the configured edit format."
                            isEnabled = pluginBasedEditsCheckBox.isSelected
                        }
                }
                row {
                    cell(autoCommitAfterEditsCheckBox)
                        .component
                        .apply {
                            text = "Auto-commit after plugin-based edits"
                            ExperimentalFeatureUtil.markAsExperimental(this)
                            toolTipText =
                                "If enabled, changes made by plugin-based edits will be automatically committed to Git with a message extracted from the LLM response."
                            isEnabled = pluginBasedEditsCheckBox.isSelected
                            addItemListener { e ->
                                val isSelected = e.stateChange == ItemEvent.SELECTED
                                if (isSelected && pluginBasedEditsCheckBox.isSelected) {
                                    // Auto-enable prompt augmentation and commit message block when auto-commit is enabled
                                    promptAugmentationCheckBox.isSelected = true
                                    includeCommitMessageBlockCheckBox.isSelected = true
                                }
                            }
                        }
                }
            }
        }
    }

    override fun apply() {
        settings.lintCmd = lintCmdField.text
        settings.editFormat = editFormatComboBox.selectedItem as String
        settings.promptAugmentation = promptAugmentationCheckBox.isSelected
        settings.includeCommitMessageBlock = includeCommitMessageBlockCheckBox.isSelected
        settings.reasoningEffort = reasoningEffortComboBox.selectedItem as String
        settings.pluginBasedEdits = pluginBasedEditsCheckBox.isSelected
        settings.lenientEdits = lenientEditsCheckBox.isSelected
        settings.autoCommitAfterEdits = autoCommitAfterEditsCheckBox.isSelected
    }

    override fun reset() {
        lintCmdField.text = settings.lintCmd
        editFormatComboBox.selectedItem = settings.editFormat
        promptAugmentationCheckBox.isSelected = settings.promptAugmentation
        includeCommitMessageBlockCheckBox.isSelected = settings.includeCommitMessageBlock
        includeCommitMessageBlockCheckBox.isEnabled = settings.promptAugmentation
        reasoningEffortComboBox.selectedItem = settings.reasoningEffort
        pluginBasedEditsCheckBox.isSelected = settings.pluginBasedEdits
        lenientEditsCheckBox.isSelected = settings.lenientEdits
        lenientEditsCheckBox.isEnabled = settings.pluginBasedEdits
        autoCommitAfterEditsCheckBox.isSelected = settings.autoCommitAfterEdits
        autoCommitAfterEditsCheckBox.isEnabled = settings.pluginBasedEdits
    }

    override fun isModified(): Boolean {
        return lintCmdField.text != settings.lintCmd ||
                editFormatComboBox.selectedItem as String != settings.editFormat ||
                promptAugmentationCheckBox.isSelected != settings.promptAugmentation ||
                includeCommitMessageBlockCheckBox.isSelected != settings.includeCommitMessageBlock ||
                reasoningEffortComboBox.selectedItem as String != settings.reasoningEffort ||
                pluginBasedEditsCheckBox.isSelected != settings.pluginBasedEdits ||
                lenientEditsCheckBox.isSelected != settings.lenientEdits ||
                autoCommitAfterEditsCheckBox.isSelected != settings.autoCommitAfterEdits
    }

    private fun createEditFormatModel(): DefaultComboBoxModel<String> {
        val formats = AiderEditFormat.values().map { it.value }.toTypedArray()
        return DefaultComboBoxModel(arrayOf("", *formats))
    }

    private fun showNotification(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(null)
    }
}
