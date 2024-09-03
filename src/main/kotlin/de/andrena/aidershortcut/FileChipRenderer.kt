package de.andrena.aidershortcut

import java.awt.Component
import java.io.File
import javax.swing.JList
import javax.swing.ListCellRenderer

class FileChipRenderer(private val readOnlyFiles: Set<File>) : ListCellRenderer<File> {
    override fun getListCellRendererComponent(
        list: JList<out File>?,
        value: File?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val isReadOnly = value?.let { it in readOnlyFiles } ?: false
        val chip = FileChip(value, isSelected, list?.selectionBackground, list?.selectionForeground, isReadOnly)
        chip.setRemoveButtonVisible(isSelected)
        return chip
    }
}
