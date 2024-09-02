package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*
import javax.swing.event.ListSelectionListener
import java.awt.GridLayout

class AiderInputDialog(project: Project, files: List<String>) : DialogWrapper(project) {
    private val inputTextField = JTextField(30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)
    private val commandOptions = arrayOf("--mini", "--sonnet", "--4", "--4o")
    private val commandComboBox = JComboBox(commandOptions)
    private val readOnlyFilesPanel = JPanel(GridLayout(0, 1)) // Vertical layout for checkboxes
    private val readOnlyToggleMap = mutableMapOf<String, JCheckBox>()

    init {
        title = "Aider Command"
        init()
        setupReadOnlyFiles(files)
    }

    private fun setupReadOnlyFiles(files: List<String>) {
        for (file in files) {
            val checkBox = JCheckBox(file)
            readOnlyToggleMap[file] = checkBox
            readOnlyFilesPanel.add(checkBox)
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS) // Vertical layout for main panel
        panel.add(inputTextField)
        panel.add(yesCheckBox)
        panel.add(commandComboBox)
        panel.add(JLabel("Select Read-only files:"))
        panel.add(readOnlyFilesPanel)
        return panel
    }

    fun getInputText(): String = inputTextField.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getSelectedCommand(): String = commandComboBox.selectedItem as String

    fun getReadOnlyFiles(): List<String> {
        return readOnlyToggleMap.filter { it.value.isSelected }.keys.toList()
    }
}
