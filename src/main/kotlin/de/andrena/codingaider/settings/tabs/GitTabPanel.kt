package de.andrena.codingaider.settings.tabs

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Panel
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker

/**
 * Git integration settings tab panel
 */
class GitTabPanel(apiKeyChecker: ApiKeyChecker) : SettingsTabPanel(apiKeyChecker) {

    // UI Components
    private val showGitComparisonToolCheckBox = JBCheckBox("Show git comparison tool after execution")
    private val includeChangeContextCheckBox = JBCheckBox("Include change context in commit messages")
    private val autoCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))
    private val dirtyCommitsComboBox = ComboBox(arrayOf("Default", "On", "Off"))

    override fun getTabName(): String = "Git"

    override fun getTabTooltip(): String = "Git integration settings"

    override fun createPanel(panel: Panel) {
        panel.apply {
            group("Git Integration") {
                row {
                    cell(showGitComparisonToolCheckBox).applyToComponent {
                        toolTipText = "When enabled, the Git comparison tool will be shown after Aider makes changes"
                    }
                }
                row("Auto-commits:") {
                    cell(autoCommitsComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Default: Use system setting. On: Aider will automatically commit changes after each successful edit. Off: Disable auto-commits."
                        }
                }
                row("Dirty-commits:") {
                    cell(dirtyCommitsComboBox)
                        .component
                        .apply {
                            toolTipText =
                                "Default: Use system setting. On: Aider will allow commits even when there are uncommitted changes in the repo. Off: Disable dirty-commits."
                        }
                }
                row {
                    cell(includeChangeContextCheckBox)
                        .component
                        .apply {
                            toolTipText =
                                "If enabled, the commit messages will include the user prompt and affected files."
                        }
                }
            }
        }
    }

    override fun apply() {
        settings.showGitComparisonTool = showGitComparisonToolCheckBox.isSelected
        settings.includeChangeContext = includeChangeContextCheckBox.isSelected
        settings.autoCommits = AiderSettings.AutoCommitSetting.fromIndex(autoCommitsComboBox.selectedIndex)
        settings.dirtyCommits = AiderSettings.DirtyCommitSetting.fromIndex(dirtyCommitsComboBox.selectedIndex)
    }

    override fun reset() {
        showGitComparisonToolCheckBox.isSelected = settings.showGitComparisonTool
        includeChangeContextCheckBox.isSelected = settings.includeChangeContext
        autoCommitsComboBox.selectedIndex = settings.autoCommits.toIndex()
        dirtyCommitsComboBox.selectedIndex = settings.dirtyCommits.toIndex()
    }

    override fun isModified(): Boolean {
        return showGitComparisonToolCheckBox.isSelected != settings.showGitComparisonTool ||
                includeChangeContextCheckBox.isSelected != settings.includeChangeContext ||
                autoCommitsComboBox.selectedIndex != settings.autoCommits.toIndex() ||
                dirtyCommitsComboBox.selectedIndex != settings.dirtyCommits.toIndex()
    }
}
