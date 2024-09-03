package de.andrena.aidershortcut

import java.awt.*
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
        leftList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        rightList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        updateLists()

        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(JLabel("Available Files:"), BorderLayout.NORTH)
        leftPanel.add(JScrollPane(leftList), BorderLayout.CENTER)

        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(JLabel("Persistent Files:"), BorderLayout.NORTH)
        rightPanel.add(JScrollPane(rightList), BorderLayout.CENTER)

        val buttonPanel = JPanel(GridBagLayout())
        val addButton = JButton(">>")
        val removeButton = JButton("<<")
        val toggleReadOnlyButton = JButton("Toggle Read-Only")

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.insets = Insets(5, 5, 5, 5)
        buttonPanel.add(addButton, gbc)

        gbc.gridy = 1
        buttonPanel.add(removeButton, gbc)

        gbc.gridy = 2
        buttonPanel.add(toggleReadOnlyButton, gbc)

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

        val mainPanel = JPanel(GridBagLayout())
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        mainPanel.add(leftPanel, gbc)

        gbc.gridx = 1
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        mainPanel.add(buttonPanel, gbc)

        gbc.gridx = 2
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.BOTH
        mainPanel.add(rightPanel, gbc)

        add(mainPanel, BorderLayout.CENTER)

        preferredSize = Dimension(800, 400)

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
            val component = list.getCellRenderer().getListCellRendererComponent(
                list,
                list.model.getElementAt(i),
                i,
                i in selectedIndices,
                false
            ) as FileChip
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
