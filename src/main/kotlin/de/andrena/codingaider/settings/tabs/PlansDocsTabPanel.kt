package de.andrena.codingaider.settings.tabs

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Panel
import de.andrena.codingaider.settings.LlmComboBoxRenderer
import de.andrena.codingaider.settings.LlmSelection
import de.andrena.codingaider.utils.ApiKeyChecker
import javax.swing.JComboBox

/**
 * Plans and documentation settings tab panel
 */
class PlansDocsTabPanel(apiKeyChecker: ApiKeyChecker) : SettingsTabPanel(apiKeyChecker) {

    // UI Components
    private val alwaysIncludePlanContextFilesCheckBox = JBCheckBox("Always include plan context files")
    private val enableAutoPlanContinueCheckBox = JBCheckBox("Enable automatic plan continuation")
    private val enableSubplansCheckBox = JBCheckBox("Enable subplans for complex features")
    private val enableDocumentationLookupCheckBox = JBCheckBox("Enable documentation lookup")
    private val planRefinementLlmComboBox: JComboBox<LlmSelection> = ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())
    private val documentationLlmComboBox: JComboBox<LlmSelection> = ComboBox(apiKeyChecker.getAllLlmOptions().toTypedArray())

    override fun getTabName(): String = "Plans & Docs"

    override fun getTabTooltip(): String = "Settings for plans and documentation"

    override fun createPanel(panel: Panel) {
        panel.apply {
            group("Plan Settings") {
                row {
                    cell(alwaysIncludePlanContextFilesCheckBox).applyToComponent {
                        toolTipText = "When enabled, files listed in the plan context will always be included"
                    }
                }
                row {
                    cell(enableAutoPlanContinueCheckBox).applyToComponent {
                        toolTipText =
                            "If enabled, plans will automatically continue when there are open checklist items"
                    }
                }
                row {
                    cell(enableSubplansCheckBox).applyToComponent {
                        toolTipText =
                            "If enabled, complex features will be broken down into subplans. Disable for simpler, single-file plans."
                    }
                }
                row("Plan Refinement LLM Model:") {
                    cell(planRefinementLlmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer(apiKeyChecker)
                        toolTipText =
                            "Select the LLM model to use for plan refinement. Leave empty to use the default LLM model."
                    }
                }
            }

            group("Documentation") {
                row {
                    cell(enableDocumentationLookupCheckBox).applyToComponent {
                        toolTipText =
                            "If enabled, documentation files (*.md) in parent directories will be included in the context"
                    }
                }
                row("Documentation LLM Model:") {
                    cell(documentationLlmComboBox).component.apply {
                        renderer = LlmComboBoxRenderer(apiKeyChecker)
                        toolTipText =
                            "Select the LLM model to use for generating documentation. The default is the LLM model specified in the settings."
                    }
                }
            }
        }
    }

    override fun apply() {
        settings.alwaysIncludePlanContextFiles = alwaysIncludePlanContextFilesCheckBox.isSelected
        settings.enableAutoPlanContinue = enableAutoPlanContinueCheckBox.isSelected
        settings.enableSubplans = enableSubplansCheckBox.isSelected
        settings.enableDocumentationLookup = enableDocumentationLookupCheckBox.isSelected
        settings.planRefinementLlm = planRefinementLlmComboBox.selectedItem.asSelectedItemName()
        settings.documentationLlm = documentationLlmComboBox.selectedItem.asSelectedItemName()
    }

    override fun reset() {
        alwaysIncludePlanContextFilesCheckBox.isSelected = settings.alwaysIncludePlanContextFiles
        enableAutoPlanContinueCheckBox.isSelected = settings.enableAutoPlanContinue
        enableSubplansCheckBox.isSelected = settings.enableSubplans
        enableDocumentationLookupCheckBox.isSelected = settings.enableDocumentationLookup
        planRefinementLlmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.planRefinementLlm)
        documentationLlmComboBox.selectedItem = apiKeyChecker.getLlmSelectionForName(settings.documentationLlm)
    }

    override fun isModified(): Boolean {
        return alwaysIncludePlanContextFilesCheckBox.isSelected != settings.alwaysIncludePlanContextFiles ||
                enableAutoPlanContinueCheckBox.isSelected != settings.enableAutoPlanContinue ||
                enableSubplansCheckBox.isSelected != settings.enableSubplans ||
                enableDocumentationLookupCheckBox.isSelected != settings.enableDocumentationLookup ||
                planRefinementLlmComboBox.selectedItem.asSelectedItemName() != settings.planRefinementLlm ||
                documentationLlmComboBox.selectedItem.asSelectedItemName() != settings.documentationLlm
    }

    fun updateLlmOptions(llmOptions: Array<LlmSelection>) {
        val currentPlanRefinementSelection = planRefinementLlmComboBox.selectedItem as? LlmSelection
        planRefinementLlmComboBox.model = javax.swing.DefaultComboBoxModel(llmOptions)
        if (currentPlanRefinementSelection != null && llmOptions.contains(currentPlanRefinementSelection)) {
            planRefinementLlmComboBox.selectedItem = currentPlanRefinementSelection
        }
        
        val currentDocumentationSelection = documentationLlmComboBox.selectedItem as? LlmSelection
        documentationLlmComboBox.model = javax.swing.DefaultComboBoxModel(llmOptions)
        if (currentDocumentationSelection != null && llmOptions.contains(currentDocumentationSelection)) {
            documentationLlmComboBox.selectedItem = currentDocumentationSelection
        }
    }

    private fun Any?.asSelectedItemName(): String {
        val selection = this as? LlmSelection ?: return ""
        return selection.name.ifBlank { "" }
    }
}
