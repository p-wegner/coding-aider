package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*

class AiderInputDialog(project: Project) : DialogWrapper(project) {
    private val inputTextField = JTextField(30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)
    private val commandOptions = arrayOf("--sonnet", "--4", "--4o", "--mini")
    private val commandComboBox = JComboBox(commandOptions)

    init {
        title = "Aider Command"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.add(inputTextField)
        panel.add(yesCheckBox)
        panel.add(commandComboBox)
        return panel
    }

    fun getInputText(): String = inputTextField.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getSelectedCommand(): String = commandComboBox.selectedItem as String
}
