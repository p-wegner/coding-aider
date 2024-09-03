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


package de.andrena.aidershortcut

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.io.File
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class FileChip(file: File?, isSelected: Boolean, selectionBackground: Color?, selectionForeground: Color?) : JPanel(BorderLayout()) {
    private val label: JLabel
    private val removeButton: JButton

    init {
        label = JLabel(file?.name ?: "")
        removeButton = JButton("×").apply {
            isVisible = false
            addActionListener { (parent as? Component)?.parent?.remove(this@FileChip) }
        }

        add(label, BorderLayout.CENTER)
        add(removeButton, BorderLayout.EAST)

        background = if (isSelected) selectionBackground else Color.WHITE
        label.foreground = if (isSelected) selectionForeground else Color.BLACK
    }

    fun setRemoveButtonVisible(visible: Boolean) {
        removeButton.isVisible = visible
    }
}
