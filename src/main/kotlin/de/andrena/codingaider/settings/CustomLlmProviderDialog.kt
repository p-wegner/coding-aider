package de.andrena.codingaider.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import javax.swing.JComponent
import com.intellij.openapi.components.service

class CustomLlmProviderDialog : DialogWrapper(null) {
    private val providerService = service<CustomLlmProviderService>()
    private val providersTableModel = ProvidersTableModel()
    private val providersTable = JBTable(providersTableModel).apply {
        setShowGrid(true)
        autoResizeMode = JBTable.AUTO_RESIZE_ALL_COLUMNS
        rowHeight = 30
        intercellSpacing = java.awt.Dimension(10, 5)
        tableHeader.preferredSize = java.awt.Dimension(0, 32)
    }

    init {
        title = "Manage Custom LLM Providers"
        init()
        setSize(800, 400)
    }

    override fun show() {
        super.show()
        providersTableModel.fireTableStructureChanged()
        adjustColumnWidths()
    }

    private fun adjustColumnWidths() {
        val totalWidth = providersTable.width
        providersTable.columnModel.apply {
            getColumn(0).preferredWidth = (totalWidth * 0.25).toInt() // Name
            getColumn(1).preferredWidth = (totalWidth * 0.15).toInt() // Type
            getColumn(2).preferredWidth = (totalWidth * 0.25).toInt() // Model
            getColumn(3).preferredWidth = (totalWidth * 0.35).toInt() // Base URL
        }
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            scrollCell(providersTable)
                .resizableColumn()
                .comment("Configure and manage your custom LLM providers")
                .preferredSize(java.awt.Dimension(750, 300))
        }
        row {
            panel {
                row {
                    button("Add Provider") { addProvider() }
                        .applyToComponent { preferredSize = java.awt.Dimension(120, 35) }
                    button("Edit Provider") { editProvider() }
                        .applyToComponent { 
                            preferredSize = java.awt.Dimension(120, 35)
                            isEnabled = providersTable.selectedRow >= 0 
                        }
                    button("Remove Provider") { removeProvider() }
                        .applyToComponent { 
                            preferredSize = java.awt.Dimension(120, 35)
                            isEnabled = providersTable.selectedRow >= 0 
                        }
                }
            }
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

}
