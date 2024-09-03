package de.andrena.aidershortcut

import java.awt.Color
import java.awt.FlowLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class FileChip(file: File?, isSelected: Boolean, selectionBackground: Color?, selectionForeground: Color?) :
    JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)) {
    private val removeButton: JButton

    init {
        isOpaque = true
        background = if (isSelected) selectionBackground else Color.LIGHT_GRAY
        foreground = if (isSelected) selectionForeground else Color.BLACK

        val label = JLabel(file?.name ?: "")
        label.toolTipText = file?.absolutePath
        add(label)

        removeButton = JButton("×")
        removeButton.isOpaque = false
        removeButton.contentAreaFilled = false
        removeButton.border = null
        removeButton.isVisible = false
        add(removeButton)
    }

    fun setRemoveButtonVisible(visible: Boolean) {
        removeButton.isVisible = visible
    }

    fun setRemoveAction(action: () -> Unit) {
        removeButton.addActionListener { action() }
    }
}