package de.andrena.codingaider.inputdialog

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import de.andrena.codingaider.services.AiderHistoryService
import java.awt.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.text.JTextComponent

data class HistoryItem(val command: List<String>, val dateTime: LocalDateTime?)

class AiderHistoryComboBox(private val project: Project, private val inputField: JTextComponent) : JComboBox<HistoryItem>() {
    private val historyService = project.service<AiderHistoryService>()

    init {
        loadHistory()
        renderer = HistoryItemRenderer()
        addActionListener {
            if (selectedIndex > 0 && selectedItem is HistoryItem) {
                val selectedItem = selectedItem as HistoryItem
                inputField.text = selectedItem.command.joinToString("\n")
            } else {
                inputField.text = ""  // Clear the input area when empty entry is selected
            }
        }
    }

    private fun loadHistory() {
        addItem(HistoryItem(emptyList(), null))  // Empty entry
        historyService.getInputHistory().forEach { (dateTime, command) ->
            addItem(HistoryItem(command, dateTime))
        }
        selectedIndex = 0  // Select the empty entry initially
    }

    fun navigateHistory(direction: Int) {
        val currentIndex = selectedIndex
        val newIndex = (currentIndex + direction).coerceIn(0, itemCount - 1)
        if (newIndex != currentIndex) {
            selectedIndex = newIndex
            val selectedItem = selectedItem as? HistoryItem
            inputField.text = selectedItem?.command?.joinToString("\n") ?: ""
        }
    }
}

private class HistoryItemRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (value is HistoryItem) {
            text = value.command.firstOrNull() ?: ""
            if (value.dateTime != null) {
                val formattedDate = value.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val fullCommand = value.command.joinToString("\n")
                toolTipText = "<html>$formattedDate<br><pre>$fullCommand</pre></html>"
            } else {
                toolTipText = null
            }
        }
        return component
    }
}
