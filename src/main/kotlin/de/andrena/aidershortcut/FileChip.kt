package de.andrena.aidershortcut

import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder

class FileChip(
    file: File?,
    isSelected: Boolean,
    selectionBackground: Color?,
    selectionForeground: Color?,
    private var isReadOnly: Boolean
) : JPanel(FlowLayout(FlowLayout.LEFT, 5, 2)) {
    private val removeButton: JButton
    private val readOnlyToggle: JToggleButton
    private val label: JLabel

    init {
        isOpaque = true
        border = EmptyBorder(2, 5, 2, 5)

        label = JLabel(file?.name ?: "")
        label.toolTipText = file?.absolutePath
        add(label)

        readOnlyToggle = JToggleButton(if (isReadOnly) "🔒" else "🔓")
        readOnlyToggle.isSelected = isReadOnly
        readOnlyToggle.isOpaque = false
        readOnlyToggle.border = null
        readOnlyToggle.font = Font(readOnlyToggle.font.name, Font.PLAIN, 12)
        readOnlyToggle.addActionListener {
            isReadOnly = readOnlyToggle.isSelected
            updateAppearance()
        }
        add(readOnlyToggle)

        removeButton = JButton("×")
        removeButton.isOpaque = false
        removeButton.setContentAreaFilled(false)
        removeButton.border = null
        removeButton.font = Font(removeButton.font.name, Font.BOLD, 14)
        removeButton.isVisible = false
        add(removeButton)

        updateColors(isSelected, selectionBackground, selectionForeground)
        updateAppearance()
    }

    fun setRemoveButtonVisible(visible: Boolean) {
        removeButton.isVisible = visible
    }

    fun setRemoveAction(action: () -> Unit) {
        removeButton.addActionListener { action() }
    }

    fun getFile(): File? {
        return label.toolTipText?.let { File(it) }
    }

    fun updateColors(isSelected: Boolean, selectionBackground: Color?, selectionForeground: Color?) {
        val defaultBackground = UIManager.getColor("List.background") ?: Color.WHITE
        val defaultForeground = UIManager.getColor("List.foreground") ?: Color.BLACK

        background = if (isSelected) selectionBackground ?: defaultBackground else defaultBackground
        foreground = if (isSelected) selectionForeground ?: defaultForeground else defaultForeground
        updateAppearance()
    }

    fun isReadOnly(): Boolean = isReadOnly

    fun setReadOnly(readOnly: Boolean) {
        isReadOnly = readOnly
        readOnlyToggle.isSelected = readOnly
        updateAppearance()
    }

    private fun updateAppearance() {
        readOnlyToggle.text = if (isReadOnly) "🔒" else "🔓"
        label.font = if (isReadOnly) label.font.deriveFont(Font.ITALIC) else label.font.deriveFont(Font.PLAIN)
        val readOnlyColor = Color(100, 100, 100)
        label.foreground = if (isReadOnly) readOnlyColor else foreground
        border = if (isReadOnly) {
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(readOnlyColor, 1),
                BorderFactory.createEmptyBorder(1, 4, 1, 4)
            )
        } else {
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        }
    }
}
