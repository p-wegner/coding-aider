package de.andrena.aidershortcut

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import javax.swing.*

class ReadOnlyFilesView(private val allFiles: List<String>, private val persistentFiles: List<String>) : JPanel() {
    private val leftList = JList<File>()
    private val rightList = JList<File>()
    private val leftModel = DefaultListModel<File>()
    private val rightModel = DefaultListModel<File>()

    init {
        layout = BorderLayout()

        leftList.model = leftModel
        rightList.model = rightModel
        leftList.cellRenderer = FileChipRenderer()
        rightList.cellRenderer = FileChipRenderer()

        updateLists()

        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(JLabel("Available Files:"), BorderLayout.NORTH)
        leftPanel.add(JScrollPane(leftList), BorderLayout.CENTER)

        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(JLabel("Persistent Files:"), BorderLayout.NORTH)
        rightPanel.add(JScrollPane(rightList), BorderLayout.CENTER)

        val buttonPanel = JPanel(GridLayout(2, 1))
        val addButton = JButton(">>")
        val removeButton = JButton("<<")

        addButton.addActionListener {
            leftList.selectedValuesList.forEach {
                leftModel.removeElement(it)
                rightModel.addElement(it)
            }
        }

        removeButton.addActionListener {
            rightList.selectedValuesList.forEach {
                rightModel.removeElement(it)
                leftModel.addElement(it)
            }
        }

        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)

        add(leftPanel, BorderLayout.WEST)
        add(buttonPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        preferredSize = Dimension(600, 300)
    }

    private fun updateLists() {
        leftModel.clear()
        rightModel.clear()

        allFiles.forEach { file ->
            if (file !in persistentFiles) {
                leftModel.addElement(File(file))
            }
        }
        persistentFiles.forEach { rightModel.addElement(File(it)) }
    }

    fun getPersistentFiles(): List<String> = rightModel.elements().toList().map { it.absolutePath }
}