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
        autoResizeMode = JBTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
        rowHeight = 30
        intercellSpacing = java.awt.Dimension(10, 5)
        tableHeader.preferredSize = java.awt.Dimension(0, 32)
        addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                adjustColumnWidths()
            }
        })
    }

    init {
        title = "Manage Custom LLM Providers"
        init()
        setSize(800, 600)
    }

    override fun show() {
        super.show()
        providersTableModel.fireTableStructureChanged()
        initializeColumns()
        adjustColumnWidths()
    }

    private fun initializeColumns() {
        providersTable.columnModel.apply {
            getColumn(0).minWidth = 150  // Name
            getColumn(1).minWidth = 100  // Type
            getColumn(2).minWidth = 120  // Model
            getColumn(3).minWidth = 200  // Base URL
        }
    }

    private fun adjustColumnWidths() {
        providersTable.columnModel.apply {
            val totalWidth = providersTable.width
            if (totalWidth > 0 && columnCount >= 4) {
                getColumn(0).width = (totalWidth * 0.25).toInt() // Name
                getColumn(1).width = (totalWidth * 0.15).toInt() // Type
                getColumn(2).width = (totalWidth * 0.20).toInt() // Model
                getColumn(3).width = (totalWidth * 0.40).toInt() // Base URL
            }
        }
    }

    override fun createCenterPanel(): JComponent = panel {
        row {
            scrollCell(providersTable)
                .resizableColumn()
                .comment("Configure and manage your custom LLM providers")
                .apply {
                    component.preferredSize = java.awt.Dimension(780, 500)
                }
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
