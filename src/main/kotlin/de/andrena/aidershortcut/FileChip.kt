package de.andrena.aidershortcut

import java.awt.Color
import java.awt.FlowLayout
import java.io.File
import javax.swing.*

class FileChip(file: File?, isSelected: Boolean, selectionBackground: Color?, selectionForeground: Color?) :
    JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)) {
    private val removeButton: JButton

    init {
        isOpaque = true
        updateColors(isSelected, selectionBackground, selectionForeground)

        val label = JLabel(file?.name ?: "")
        label.toolTipText = file?.absolutePath
        add(label)

        removeButton = JButton("×")
        removeButton.isOpaque = false
        removeButton.setContentAreaFilled(false)
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

    fun getFile(): File? {
        return (getComponent(0) as? JLabel)?.toolTipText?.let { File(it) }
    }

    fun updateColors(isSelected: Boolean, selectionBackground: Color?, selectionForeground: Color?) {
        val defaultBackground = UIManager.getColor("List.background") ?: Color.WHITE
        val defaultForeground = UIManager.getColor("List.foreground") ?: Color.BLACK

        background = if (isSelected) selectionBackground ?: defaultBackground else defaultBackground
        foreground = if (isSelected) selectionForeground ?: defaultForeground else defaultForeground
    }
}
