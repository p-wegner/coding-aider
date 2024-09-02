package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class AiderInputDialog(project: Project) : DialogWrapper(project) {
    private val inputTextField = JTextField(30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)
    private val commandOptions = arrayOf("--mini","--sonnet", "--4", "--4o")
    private val commandComboBox = JComboBox(commandOptions)
    private val readOnlyFilesField = JTextField(30)

    init {
        title = "Aider Command"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.add(inputTextField)
        panel.add(yesCheckBox)
        panel.add(commandComboBox)
        panel.add(JLabel("Read-only files (for --read):"))
        panel.add(readOnlyFilesField)
        return panel
    }

    fun getInputText(): String = inputTextField.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getSelectedCommand(): String = commandComboBox.selectedItem as String
    fun getReadOnlyFiles(): String = readOnlyFilesField.text
}
