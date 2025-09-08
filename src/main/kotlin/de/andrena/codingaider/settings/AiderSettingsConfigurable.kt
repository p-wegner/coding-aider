package de.andrena.codingaider.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.tabs.impl.JBEditorTabs
import de.andrena.codingaider.settings.tabs.*
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * Configurable for Aider settings
 */
class AiderSettingsConfigurable : Configurable {

    private val apiKeyChecker: ApiKeyChecker = service<DefaultApiKeyChecker>()
    private var settingsComponent: JPanel? = null
    private val tabsDisposable: Disposable = Disposer.newDisposable()
    private val tabsComponent = JBEditorTabs(null, tabsDisposable)
    private val settings = AiderSettings.getInstance()
    private val customProviderService = getApplication().getService(CustomLlmProviderService::class.java)

    // Tab panels
    private val setupPanel = AiderSetupPanel(apiKeyChecker) { useDockerAider ->
        // Update sidecar mode checkbox in advanced tab when docker mode changes
        if (useDockerAider) {
            advancedPanel.disableSidecarMode()
        } else {
            advancedPanel.enableSidecarMode()
        }
    }
    private val generalPanel = GeneralSettingsTabPanel(apiKeyChecker)
    private val codeModificationPanel = CodeModificationTabPanel(apiKeyChecker)
    private val gitPanel = GitTabPanel(apiKeyChecker)
    private val plansDocsPanel = PlansDocsTabPanel(apiKeyChecker)
    private val advancedPanel = AdvancedTabPanel(apiKeyChecker)

    // Listener for custom provider changes
    private val customProviderListener: () -> Unit = { updateLlmOptions() }

    override fun getDisplayName(): String = "Aider"

    override fun createComponent(): JPanel {
        val mainPanel = JPanel(BorderLayout())

        // Create tabs
        val setupTab = setupPanel.createTabInfo()
        val generalTab = generalPanel.createTabInfo()
        val codeModificationTab = codeModificationPanel.createTabInfo()
        val gitTab = gitPanel.createTabInfo()
        val planTab = plansDocsPanel.createTabInfo()
        val advancedTab = advancedPanel.createTabInfo()

        // Add tabs to the tabbed pane
        tabsComponent.addTab(setupTab)
        tabsComponent.addTab(generalTab)
        tabsComponent.addTab(codeModificationTab)
        tabsComponent.addTab(gitTab)
        tabsComponent.addTab(planTab)
        tabsComponent.addTab(advancedTab)

        mainPanel.add(tabsComponent.component, BorderLayout.CENTER)
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Register listener for custom provider changes
        customProviderService.addSettingsChangeListener(customProviderListener)

        settingsComponent = mainPanel
        return mainPanel
    }

    override fun isModified(): Boolean {
        return setupPanel.isModified() ||
                generalPanel.isModified() ||
                codeModificationPanel.isModified() ||
                gitPanel.isModified() ||
                plansDocsPanel.isModified() ||
                advancedPanel.isModified()
    }

    override fun apply() {
        setupPanel.apply()
        generalPanel.apply()
        codeModificationPanel.apply()
        gitPanel.apply()
        plansDocsPanel.apply()
        advancedPanel.apply()
        
        // Notify listeners that settings have changed
        settings.notifySettingsChanged()
    }

    override fun reset() {
        setupPanel.reset()
        generalPanel.reset()
        codeModificationPanel.reset()
        gitPanel.reset()
        plansDocsPanel.reset()
        advancedPanel.reset()
    }

    override fun disposeUIResources() {
        setupPanel.updateApiKeyFieldsOnClose()
        customProviderService.removeSettingsChangeListener(customProviderListener)
        
        // Dispose all tab panels
        setupPanel.dispose()
        generalPanel.dispose()
        codeModificationPanel.dispose()
        gitPanel.dispose()
        plansDocsPanel.dispose()
        advancedPanel.dispose()
        
        settingsComponent = null
        
        // Ensure that any pending changes are saved
        if (isModified) {
            apply()
        }
    }

    private fun updateLlmOptions() {
        val llmOptions = apiKeyChecker.getAllLlmOptions().toTypedArray()
        generalPanel.updateLlmOptions()
        plansDocsPanel.updateLlmOptions(llmOptions)
        advancedPanel.updateLlmOptions(llmOptions)
    }
}
