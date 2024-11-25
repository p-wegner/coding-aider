package de.andrena.codingaider.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.utils.DefaultApiKeyChecker

class CustomLlmProviderDialog : DialogWrapper(null) {
    private val providerService = CustomLlmProviderService.getInstance()
    private val providersListModel = DefaultListModel<String>()
    private val providersList = JBList(providersListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    private val addButton = javax.swing.JButton("Add Provider").apply {
        addActionListener { addProvider() }
        mnemonic = java.awt.event.KeyEvent.VK_A
    }

    private val editButton = javax.swing.JButton("Edit Provider").apply {
        addActionListener { editProvider() }
        mnemonic = java.awt.event.KeyEvent.VK_E
        isEnabled = false
    }

    private val copyButton = javax.swing.JButton("Copy Provider").apply {
        addActionListener { copyProvider() }
        mnemonic = java.awt.event.KeyEvent.VK_C
        isEnabled = false
    }

    private val removeButton = javax.swing.JButton("Remove Provider").apply {
        addActionListener { removeProvider() }
        mnemonic = java.awt.event.KeyEvent.VK_R
        isEnabled = false
    }

    private val hideButton = javax.swing.JButton("Hide Provider").apply {
        addActionListener { toggleProviderVisibility() }
        mnemonic = java.awt.event.KeyEvent.VK_H
        isEnabled = false
    }

    init {
        title = "Manage Custom LLM Providers"
        init()
        setSize(600, 400)
        updateProvidersList()

        providersList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selectedValue = providersList.selectedValue
                val isBuiltIn = selectedValue?.startsWith("[Built-in]") ?: false
                val hasSelection = providersList.selectedIndex != -1
                val defaultSettings = service<DefaultProviderSettings>()

                val provider = getSelectedProvider()
                val selectedLlm = if (isBuiltIn) getSelectedBuiltInProvider() else null
                
                editButton.isEnabled = hasSelection && !isBuiltIn
                copyButton.isEnabled = hasSelection
                removeButton.isEnabled = hasSelection && !isBuiltIn
                hideButton.isEnabled = hasSelection
                hideButton.text = when {
                    provider?.hidden == true -> "Show Provider"
                    selectedLlm?.let { defaultSettings.hiddenProviders.contains(it) } == true -> "Show Provider"
                    else -> "Hide Provider"
                }
            }
        }
    }

    private fun updateProvidersList() {
        providersListModel.clear()
        // Add built-in providers
        val defaultSettings = service<DefaultProviderSettings>()
        service<DefaultApiKeyChecker>().getAllStandardLlmKeys().forEach { llm ->
            val prefix = if (defaultSettings.hiddenProviders.contains(llm)) "[Built-in] [Hidden]" else "[Built-in]"
            providersListModel.addElement("$prefix $llm")
        }
        // Add custom providers
        providerService.getAllProviders().forEach { provider ->
            providersListModel.addElement(
                "${if (provider.hidden) "[Hidden] " else ""}${provider.name} (${provider.type}) - ${provider.modelName}"
            )
        }
    }

    private fun getSelectedProvider(): CustomLlmProvider? {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex < 0) return null

        val builtInCount = service<DefaultApiKeyChecker>().getAllStandardLlmKeys().size
        if (selectedIndex < builtInCount) return null

        return providerService.getAllProviders()[selectedIndex - builtInCount]
    }

    private fun getSelectedBuiltInProvider(): String? {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex < 0) return null
        
        val builtInProviders = service<DefaultApiKeyChecker>().getAllStandardLlmKeys()
        return if (selectedIndex < builtInProviders.size) builtInProviders[selectedIndex] else null
    }

    private fun toggleProviderVisibility() {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex < 0) return

        val defaultSettings = service<DefaultProviderSettings>()
        val builtInProvider = getSelectedBuiltInProvider()
        
        if (builtInProvider != null) {
            if (defaultSettings.hiddenProviders.contains(builtInProvider)) {
                defaultSettings.hiddenProviders.remove(builtInProvider)
            } else {
                defaultSettings.hiddenProviders.add(builtInProvider)
            }
        } else {
            val provider = getSelectedProvider() ?: return
            providerService.toggleProviderVisibility(provider.name)
        }
        updateProvidersList()
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            cell(JBScrollPane(providersList))
                .comment("Configure and manage your custom LLM providers")
                .apply {
                    component.preferredSize = java.awt.Dimension(580, 300)
                }
        }
        row {
            cell(addButton)
            cell(editButton)
            cell(copyButton)
            cell(removeButton)
            cell(hideButton)
        }
    }

    private fun addProvider() {
        val dialog = CustomLlmProviderEditorDialog()
        if (dialog.showAndGet()) {
            val provider = dialog.getProvider()
            providerService.addProvider(provider)
            updateProvidersList()
        }
    }

    private fun editProvider() {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex < 0) return

        val builtInCount = service<DefaultApiKeyChecker>().getAllStandardLlmKeys().size
        if (selectedIndex < builtInCount) return // Built-in provider

        val provider = providerService.getAllProviders()[selectedIndex - builtInCount]
        val dialog = CustomLlmProviderEditorDialog(provider)
        if (dialog.showAndGet()) {
            providerService.removeProvider(provider.name)
            providerService.addProvider(dialog.getProvider())
            updateProvidersList()
        }
    }

    private fun copyProvider() {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex < 0) return

        val builtInCount = service<DefaultApiKeyChecker>().getAllStandardLlmKeys().size
        val provider = if (selectedIndex < builtInCount) {
            // Create a dummy provider for built-in LLM
            val llmName = service<DefaultApiKeyChecker>().getAllStandardLlmKeys()[selectedIndex]
            CustomLlmProvider(
                name = llmName,
                type = LlmProviderType.OPENAI, // Default to OpenAI for built-in
                modelName = llmName,
                baseUrl = "",
            )
        } else {
            providerService.getAllProviders()[selectedIndex - builtInCount]
        }
        val copiedProvider = provider.copy(
            name = "${provider.name} (Copy)",
        )
        val dialog = CustomLlmProviderEditorDialog(copiedProvider)
        if (dialog.showAndGet()) {
            providerService.addProvider(dialog.getProvider())
            updateProvidersList()
        }
    }

    private fun removeProvider() {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex < 0) return

        val builtInCount = service<DefaultApiKeyChecker>().getAllStandardLlmKeys().size
        if (selectedIndex < builtInCount) return // Can't remove built-in provider

        val provider = providerService.getAllProviders()[selectedIndex - builtInCount]
        providerService.removeProvider(provider.name)
        updateProvidersList()
    }
}
