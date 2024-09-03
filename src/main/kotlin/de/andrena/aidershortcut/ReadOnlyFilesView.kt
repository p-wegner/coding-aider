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
    private val readOnlyFiles = mutableSetOf<File>()

    init {
        layout = BorderLayout()

        leftList.model = leftModel
        rightList.model = rightModel
        leftList.cellRenderer = FileChipRenderer(readOnlyFiles)
        rightList.cellRenderer = FileChipRenderer(readOnlyFiles)

        updateLists()

        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(JLabel("Available Files:"), BorderLayout.NORTH)
        leftPanel.add(JScrollPane(leftList), BorderLayout.CENTER)

        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(JLabel("Persistent Files:"), BorderLayout.NORTH)
        rightPanel.add(JScrollPane(rightList), BorderLayout.CENTER)

        val buttonPanel = JPanel(GridLayout(3, 1))
        val addButton = JButton(">>")
        val removeButton = JButton("<<")
        val toggleReadOnlyButton = JButton("Toggle Read-Only")

        addButton.addActionListener {
            leftList.selectedValuesList.forEach { file ->
                moveFileToRightList(file)
            }
        }

        removeButton.addActionListener {
            rightList.selectedValuesList.forEach { file ->
                moveFileToLeftList(file)
            }
        }

        toggleReadOnlyButton.addActionListener {
            val selectedFiles = leftList.selectedValuesList + rightList.selectedValuesList
            selectedFiles.forEach { file ->
                toggleReadOnly(file)
            }
            leftList.repaint()
            rightList.repaint()
        }

        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)
        buttonPanel.add(toggleReadOnlyButton)

        add(leftPanel, BorderLayout.WEST)
        add(buttonPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        preferredSize = Dimension(600, 300)

        setupRemoveButtons()
    }

    private fun toggleReadOnly(file: File) {
        if (file in readOnlyFiles) {
            readOnlyFiles.remove(file)
        } else {
            readOnlyFiles.add(file)
        }
    }

    private fun setupRemoveButtons() {
        leftList.addListSelectionListener {
            updateRemoveButtons(leftList, leftModel)
        }
        rightList.addListSelectionListener {
            updateRemoveButtons(rightList, rightModel)
        }
    }

    private fun updateRemoveButtons(list: JList<File>, model: DefaultListModel<File>) {
        val selectedIndices = list.selectedIndices
        for (i in 0 until list.model.size) {
            val component = list.getCellRenderer().getListCellRendererComponent(list, list.model.getElementAt(i), i, i in selectedIndices, false) as FileChip
            component.setRemoveButtonVisible(i in selectedIndices)
            component.setRemoveAction {
                val file = component.getFile()
                if (file != null) {
                    if (model == rightModel) {
                        moveFileToLeftList(file)
                    } else {
                        moveFileToRightList(file)
                    }
                }
            }
        }
        list.repaint()
    }

    private fun updateLists() {
        leftModel.clear()
        rightModel.clear()

        val persistentSet = persistentFiles.map { File(it) }.toSet()
        
        // Add persistent files to the right list
        persistentSet.forEach { rightModel.addElement(it) }

        // Add non-persistent files to the left list
        allFiles.map { File(it) }
            .filter { it !in persistentSet }
            .forEach { leftModel.addElement(it) }
    }

    private fun moveFileToRightList(file: File) {
        if (leftModel.contains(file)) {
            leftModel.removeElement(file)
        }
        if (!rightModel.contains(file)) {
            rightModel.addElement(file)
        }
    }

    private fun moveFileToLeftList(file: File) {
        if (rightModel.contains(file)) {
            rightModel.removeElement(file)
        }
        if (!leftModel.contains(file)) {
            leftModel.addElement(file)
        }
    }

    fun getPersistentFiles(): List<String> = rightModel.elements().toList().map { it.absolutePath }
}
