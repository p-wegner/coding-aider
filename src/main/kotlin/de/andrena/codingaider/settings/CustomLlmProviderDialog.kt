package de.andrena.codingaider.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import com.intellij.openapi.components.service
import com.intellij.ui.components.JBScrollPane

class CustomLlmProviderDialog : DialogWrapper(null) {
    private val providerService = service<CustomLlmProviderService>()
    private val providersListModel = DefaultListModel<String>()
    private val providersList = JBList(providersListModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    private val addButton = javax.swing.JButton("Add Provider").apply {
        addActionListener { addProvider() }
    }

    private val editButton = javax.swing.JButton("Edit Provider").apply {
        addActionListener { editProvider() }
        isEnabled = false
    }

    private val copyButton = javax.swing.JButton("Copy Provider").apply {
        addActionListener { copyProvider() }
        isEnabled = false
    }

    private val removeButton = javax.swing.JButton("Remove Provider").apply {
        addActionListener { removeProvider() }
        isEnabled = false
    }

    init {
        title = "Manage Custom LLM Providers"
        init()
        setSize(600, 400)
        updateProvidersList()

        providersList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val hasSelection = providersList.selectedIndex != -1
                editButton.isEnabled = hasSelection
                copyButton.isEnabled = hasSelection
                removeButton.isEnabled = hasSelection
            }
        }
    }

    private fun updateProvidersList() {
        providersListModel.clear()
        providerService.getAllProviders().forEach { provider ->
            providersListModel.addElement(
                "${provider.name} (${provider.type}) - ${provider.modelName}"
            )
        }
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
        if (selectedIndex >= 0) {
            val provider = providerService.getAllProviders()[selectedIndex]
            val dialog = CustomLlmProviderEditorDialog(provider)
            if (dialog.showAndGet()) {
                providerService.removeProvider(provider.name)
                providerService.addProvider(dialog.getProvider())
                updateProvidersList()
            }
        }
    }

    private fun copyProvider() {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex >= 0) {
            val provider = providerService.getAllProviders()[selectedIndex]
            val copiedProvider = provider.copy(
                name = "${provider.name} (Copy)",
                displayName = provider.displayName?.let { "$it (Copy)" }
            )
            val dialog = CustomLlmProviderEditorDialog(copiedProvider)
            if (dialog.showAndGet()) {
                providerService.addProvider(dialog.getProvider())
                updateProvidersList()
            }
        }
    }

    private fun removeProvider() {
        val selectedIndex = providersList.selectedIndex
        if (selectedIndex >= 0) {
            val provider = providerService.getAllProviders()[selectedIndex]
            providerService.removeProvider(provider.name)
            updateProvidersList()
        }
    }
}
