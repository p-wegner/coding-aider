package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class AiderInputDialog(project: Project) : DialogWrapper(project) {
    private val inputTextField = JTextField(30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)

    init {
        title = "Aider Command"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.add(inputTextField)
        panel.add(yesCheckBox)
        return panel
    }

    fun getInputText(): String = inputTextField.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
}