package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*
import javax.swing.event.ListSelectionListener

class AiderInputDialog(project: Project, files: List<String>) : DialogWrapper(project) {
    private val inputTextField = JTextField(30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)
    private val commandOptions = arrayOf("--mini", "--sonnet", "--4", "--4o")
    private val commandComboBox = JComboBox(commandOptions)
    private val readOnlyFilesList = JList<String>(DefaultListModel<String>())
    private val readOnlyToggleMap = mutableMapOf<String, Boolean>()

    init {
        title = "Aider Command"
        init()
        setupReadOnlyFiles(files)
    }

    private fun setupReadOnlyFiles(files: List<String>) {
        val model = readOnlyFilesList.model as DefaultListModel<String>
        for (file in files) {
            model.addElement(file)
            readOnlyToggleMap[file] = false // Default to not read-only
        }
        readOnlyFilesList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selectedFile = readOnlyFilesList.selectedValue
                if (selectedFile != null) {
                    readOnlyToggleMap[selectedFile] = !readOnlyToggleMap[selectedFile]!!
                }
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.add(inputTextField)
        panel.add(yesCheckBox)
        panel.add(commandComboBox)
        panel.add(JLabel("Select Read-only files:"))
        panel.add(JScrollPane(readOnlyFilesList))
        return panel
    }

    fun getInputText(): String = inputTextField.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getSelectedCommand(): String = commandComboBox.selectedItem as String

    fun getReadOnlyFiles(): List<String> {
        return readOnlyToggleMap.filter { it.value }.keys.toList()
    }
}
