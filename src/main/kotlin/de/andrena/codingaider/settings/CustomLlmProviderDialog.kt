package de.andrena.codingaider.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel
import com.intellij.openapi.components.service

class CustomLlmProviderDialog : DialogWrapper(null) {
    private val providerService = service<CustomLlmProviderService>()
    private val providersTableModel = ProvidersTableModel()
    private val providersTable = JBTable(providersTableModel)

    init {
        title = "Manage Custom LLM Providers"
        init()
        providersTable.setShowGrid(true)
    }

    override fun show() {
        super.show()
        if (providersTable.columnModel.columnCount >= 3) {
            providersTable.columnModel.getColumn(0).preferredWidth = 150
            providersTable.columnModel.getColumn(1).preferredWidth = 100
            providersTable.columnModel.getColumn(2).preferredWidth = 200
        }
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            scrollCell(providersTable)
                .resizableColumn()
                .comment("List of configured LLM providers")
        }
        row {
            button("Add Provider") { addProvider() }
            button("Edit Provider") { editProvider() }
            button("Remove Provider") { removeProvider() }
        }
    }

    private fun addProvider() {
        val dialog = CustomLlmProviderEditorDialog()
        if (dialog.showAndGet()) {
            val provider = dialog.getProvider()
            providerService.addProvider(provider)
            providersTableModel.fireTableDataChanged()
        }
    }

    private fun editProvider() {
        val selectedRow = providersTable.selectedRow
        if (selectedRow >= 0) {
            val provider = providerService.getAllProviders()[selectedRow]
            val dialog = CustomLlmProviderEditorDialog(provider)
            if (dialog.showAndGet()) {
                providerService.removeProvider(provider.name)
                providerService.addProvider(dialog.getProvider())
                providersTableModel.fireTableDataChanged()
            }
        }
    }

    private fun removeProvider() {
        val selectedRow = providersTable.selectedRow
        if (selectedRow >= 0) {
            val provider = providerService.getAllProviders()[selectedRow]
            providerService.removeProvider(provider.name)
            providersTableModel.fireTableDataChanged()
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (nameField.text.isBlank()) {
            return ValidationInfo("Provider name is required", nameField)
        }
        if (modelNameField.text.isBlank()) {
            return ValidationInfo("Model name is required", modelNameField)
        }
        val selectedType = providerTypeComboBox.selectedItem as LlmProviderType
        if (selectedType.requiresBaseUrl && baseUrlField.text.isBlank()) {
            return ValidationInfo("Base URL is required for ${selectedType.name}", baseUrlField)
        }
        return null
    }

    fun getProvider(): CustomLlmProvider {
        val type: LlmProviderType = providerTypeComboBox.selectedItem as LlmProviderType
        return CustomLlmProvider(
            name = nameField.text,
            displayName = displayNameField.text.takeIf { it.isNotBlank() },
            type = type,
            baseUrl = baseUrlField.text?:"",
            modelName = modelNameField?.text ?:"",
        )
    }
}
