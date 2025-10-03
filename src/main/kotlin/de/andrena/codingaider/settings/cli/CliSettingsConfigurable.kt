package de.andrena.codingaider.settings.cli

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.tabs.impl.JBEditorTabs
import de.andrena.codingaider.settings.*
import de.andrena.codingaider.settings.tabs.*
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Configurable for CLI settings with support for multiple CLI tools
 */
class CliSettingsConfigurable : Configurable {

    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
    private var settingsComponent: JPanel? = null
    private val tabsDisposable: Disposable = Disposer.newDisposable()
    private val tabsComponent = JBEditorTabs(null, tabsDisposable)
    
    // CLI Selection
    private val cliSelectionPanel = CliSelectionPanel()
    
    // Aider Settings (existing)
    private val aiderSettings = AiderSettings.getInstance()
    private val customProviderService = com.intellij.openapi.application.ApplicationManager.getApplication()
        .getService(CustomLlmProviderService::class.java)
    
    // Aider tab panels
    private val aiderSetupPanel = AiderSetupPanel(apiKeyChecker) { useDockerAider ->
        // Update sidecar mode checkbox in advanced tab when docker mode changes
        if (useDockerAider) {
            aiderAdvancedPanel.disableSidecarMode()
        } else {
            aiderAdvancedPanel.enableSidecarMode()
        }
    }
    private val aiderGeneralPanel = GeneralSettingsTabPanel(apiKeyChecker)
    private val aiderCodeModificationPanel = CodeModificationTabPanel(apiKeyChecker)
    private val aiderGitPanel = GitTabPanel(apiKeyChecker)
    private val aiderPlansDocsPanel = PlansDocsTabPanel(apiKeyChecker)
    private val aiderAdvancedPanel = AdvancedTabPanel(apiKeyChecker)
    
    // Claude Code Settings
    private val claudeCodeConfigPanel = ClaudeCodeConfigurationPanel()
    
    // Card layout for switching between CLI-specific settings
    private val cliSettingsPanel = JPanel(CardLayout())
    private val cardLayout = cliSettingsPanel.layout as CardLayout
    
    // Listener for custom provider changes
    private val customProviderListener: () -> Unit = { updateLlmOptions() }
    private val cliSelectionListener: () -> Unit = { switchToSelectedCli() }

    override fun getDisplayName(): String = "CLI Tools"

    override fun createComponent(): JPanel {
        val mainPanel = JPanel(BorderLayout())

        // Add CLI selection panel at the top
        val cliSelectionComponent = cliSelectionPanel.createPanel()
        mainPanel.add(cliSelectionComponent, BorderLayout.NORTH)

        // Add card panel for CLI-specific settings
        mainPanel.add(cliSettingsPanel, BorderLayout.CENTER)

        // Create Aider tabs panel
        val aiderTabsPanel = JPanel(BorderLayout())
        val aiderTabsComponent = JBEditorTabs(null, tabsDisposable)
        
        val aiderSetupTab = aiderSetupPanel.createTabInfo()
        val aiderGeneralTab = aiderGeneralPanel.createTabInfo()
        val aiderCodeModificationTab = aiderCodeModificationPanel.createTabInfo()
        val aiderGitTab = aiderGitPanel.createTabInfo()
        val aiderPlanTab = aiderPlansDocsPanel.createTabInfo()
        val aiderAdvancedTab = aiderAdvancedPanel.createTabInfo()

        aiderTabsComponent.addTab(aiderSetupTab)
        aiderTabsComponent.addTab(aiderGeneralTab)
        aiderTabsComponent.addTab(aiderCodeModificationTab)
        aiderTabsComponent.addTab(aiderGitTab)
        aiderTabsComponent.addTab(aiderPlanTab)
        aiderTabsComponent.addTab(aiderAdvancedTab)

        aiderTabsPanel.add(aiderTabsComponent.component, BorderLayout.CENTER)

        // Add CLI-specific panels to card layout
        cliSettingsPanel.add(aiderTabsPanel, CliType.AIDER.name)
        cliSettingsPanel.add(claudeCodeConfigPanel.createPanel(), CliType.CLAUDE_CODE.name)

        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Register listeners
        customProviderService.addSettingsChangeListener(customProviderListener)
        cliSelectionPanel.addSettingsChangeListener(cliSelectionListener)

        // Initialize with current selection
        switchToSelectedCli()

        settingsComponent = mainPanel
        return mainPanel
    }

    private fun switchToSelectedCli() {
        val selectedCli = GenericCliSettings.getInstance().getSelectedCli()
        cardLayout.show(cliSettingsPanel, selectedCli.name)
    }

    override fun isModified(): Boolean {
        return cliSelectionPanel.isModified() ||
                when (GenericCliSettings.getInstance().getSelectedCli()) {
                    CliType.AIDER -> isAiderModified()
                    CliType.CLAUDE_CODE -> claudeCodeConfigPanel.isModified()
                    else -> false
                }
    }

    private fun isAiderModified(): Boolean {
        return aiderSetupPanel.isModified() ||
                aiderGeneralPanel.isModified() ||
                aiderCodeModificationPanel.isModified() ||
                aiderGitPanel.isModified() ||
                aiderPlansDocsPanel.isModified() ||
                aiderAdvancedPanel.isModified()
    }

    override fun apply() {
        cliSelectionPanel.apply()
        
        when (GenericCliSettings.getInstance().getSelectedCli()) {
            CliType.AIDER -> {
                aiderSetupPanel.apply()
                aiderGeneralPanel.apply()
                aiderCodeModificationPanel.apply()
                aiderGitPanel.apply()
                aiderPlansDocsPanel.apply()
                aiderAdvancedPanel.apply()
                aiderSettings.notifySettingsChanged()
            }
            CliType.CLAUDE_CODE -> {
                claudeCodeConfigPanel.apply()
            }
            else -> {}
        }
    }

    override fun reset() {
        cliSelectionPanel.reset()
        
        when (GenericCliSettings.getInstance().getSelectedCli()) {
            CliType.AIDER -> {
                aiderSetupPanel.reset()
                aiderGeneralPanel.reset()
                aiderCodeModificationPanel.reset()
                aiderGitPanel.reset()
                aiderPlansDocsPanel.reset()
                aiderAdvancedPanel.reset()
            }
            CliType.CLAUDE_CODE -> {
                claudeCodeConfigPanel.reset()
            }
            else -> {}
        }
    }

    override fun disposeUIResources() {
        cliSelectionPanel.dispose()
        customProviderService.removeSettingsChangeListener(customProviderListener)
        
        // Dispose Aider panels
        aiderSetupPanel.updateApiKeyFieldsOnClose()
        aiderSetupPanel.dispose()
        aiderGeneralPanel.dispose()
        aiderCodeModificationPanel.dispose()
        aiderGitPanel.dispose()
        aiderPlansDocsPanel.dispose()
        aiderAdvancedPanel.dispose()
        
        // Dispose Claude Code panel
        // Note: ClaudeCodeConfigurationPanel doesn't have a dispose method currently
        
        settingsComponent = null
        
        // Ensure that any pending changes are saved
        if (isModified) {
            apply()
        }
    }

    private fun updateLlmOptions() {
        val llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
        aiderGeneralPanel.updateLlmOptions()
        aiderPlansDocsPanel.updateLlmOptions(llmOptions)
        aiderAdvancedPanel.updateLlmOptions(llmOptions)
    }
}