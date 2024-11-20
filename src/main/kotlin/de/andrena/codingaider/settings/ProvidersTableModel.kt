package de.andrena.codingaider.settings

import com.intellij.openapi.components.service
import javax.swing.table.AbstractTableModel

class ProvidersTableModel : AbstractTableModel() {
    private val providerService = service<CustomLlmProviderService>()
    private val columnNames = arrayOf("Name", "Type", "Model", "Base URL")

    override fun getRowCount(): Int = providerService.getAllProviders().size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val provider = providerService.getAllProviders()[rowIndex]
        return when (columnIndex) {
            0 -> provider.displayName ?: provider.name
            1 -> provider.type.displayName
            2 -> provider.modelName
            3 -> provider.baseUrl
            else -> ""
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java
}
