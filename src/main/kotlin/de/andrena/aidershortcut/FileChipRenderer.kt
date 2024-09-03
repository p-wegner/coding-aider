package de.andrena.aidershortcut

import java.awt.Component
import java.io.File
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class FileChipRenderer : ListCellRenderer<File> {
    private val label = JLabel()

    init {
        label.isOpaque = true
    }

    override fun getListCellRendererComponent(
        list: JList<out File>?,
        value: File?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        label.text = value?.name ?: ""
        label.toolTipText = value?.absolutePath

        if (isSelected) {
            label.background = list?.selectionBackground
            label.foreground = list?.selectionForeground
        } else {
            label.background = list?.background
            label.foreground = list?.foreground
        }

        return label
    }
}