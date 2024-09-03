package de.andrena.aidershortcut

import java.awt.Component
import java.io.File
import javax.swing.JList
import javax.swing.ListCellRenderer

class FileChipRenderer : ListCellRenderer<File> {
    override fun getListCellRendererComponent(
        list: JList<out File>?,
        value: File?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val chip = FileChip(value, isSelected, list?.selectionBackground, list?.selectionForeground)
        chip.setRemoveButtonVisible(isSelected)
        return chip
    }
}
