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
            for (i in 0 until columnCount) {
                val column = getColumn(i)
                when (i) {
                    0 -> { // Name
                        column.minWidth = 150
                        column.preferredWidth = 200
                    }
                    1 -> { // Type
                        column.minWidth = 100
                        column.preferredWidth = 120
                    }
                    2 -> { // Model
                        column.minWidth = 120
                        column.preferredWidth = 160
                    }
                    3 -> { // Base URL
                        column.minWidth = 200
                        column.preferredWidth = 320
                    }
                }
            }
        }
    }

    private fun adjustColumnWidths() {
        providersTable.columnModel.apply {
            val totalWidth = providersTable.width
            if (totalWidth > 0 && columnCount >= 4) {
                val widths = listOf(0.25, 0.15, 0.20, 0.40)
                for (i in 0 until minOf(columnCount, widths.size)) {
                    val calculatedWidth = (totalWidth * widths[i]).toInt()
                    val column = getColumn(i)
                    if (calculatedWidth >= column.minWidth) {
                        column.width = calculatedWidth
                    }
                }
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
