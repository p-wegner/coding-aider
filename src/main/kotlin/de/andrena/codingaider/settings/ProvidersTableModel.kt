package de.andrena.codingaider.settings

import com.intellij.openapi.components.service
import javax.swing.table.AbstractTableModel

class ProvidersTableModel : AbstractTableModel() {
    private val providerService = service<CustomLlmProviderService>()
    private val columnNames = arrayOf("Name", "Type", "Model")

    override fun getRowCount(): Int = providerService.getAllProviders().size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(row: Int, column: Int): Any {
        val provider = providerService.getAllProviders()[row]
        return when (column) {
            0 -> provider.displayName ?: provider.name
            1 -> provider.type.name
            2 -> provider.modelName
            else -> ""
        }
    }
}
