package de.andrena.codingaider.dialogs

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettings
import javax.swing.JComponent

class TestTypeDialog(
    private val project: Project,
    private val existing: AiderProjectSettings.TestTypeConfiguration?
) : DialogWrapper(project) {
    
    private val nameField = JBTextField()
    private val promptTemplateArea = JBTextArea().apply {
        rows = 5
        lineWrap = true
        wrapStyleWord = true
    }
    private val referencePatternField = JBTextField()
    private val testPatternField = JBTextField()
    private val enabledCheckBox = JBCheckBox("Enabled")
    private val contextFilesList = com.intellij.ui.components.JBList<String>()
    private val contextFilesModel = javax.swing.DefaultListModel<String>()
    
    init {
        title = if (existing == null) "Add Test Type" else "Edit Test Type"
        contextFilesList.model = contextFilesModel
        
        existing?.let {
            nameField.text = it.name
            promptTemplateArea.text = it.promptTemplate
            referencePatternField.text = it.referenceFilePattern
            testPatternField.text = it.testFilePattern
            enabledCheckBox.isSelected = it.isEnabled
            it.contextFiles.forEach { file -> contextFilesModel.addElement(file) }
        }
        init()
    }

    private fun addContextFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
        val files = FileChooser.chooseFiles(descriptor, project, null)
        files.forEach { file ->
            if (!contextFilesModel.contains(file.path)) {
                contextFilesModel.addElement(file.path)
            }
        }
    }

    private fun removeSelectedContextFiles() {
        contextFilesList.selectedValuesList.forEach { contextFilesModel.removeElement(it) }
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row("Name:") {
                cell(nameField)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row("Prompt Template:") {
                cell(JBScrollPane(promptTemplateArea))
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignY.FILL)
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }.resizableRow()
            row("Reference File Pattern:") {
                cell(referencePatternField)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row("Test File Pattern:") {
                cell(testPatternField)
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row {
                cell(enabledCheckBox)
            }
            row("Context Files:") {
                cell(JBScrollPane(contextFilesList))
                    .resizableColumn()
                    .align(com.intellij.ui.dsl.builder.AlignY.FILL)
                    .align(com.intellij.ui.dsl.builder.AlignX.FILL)
            }
            row {
                button("Add Files") { addContextFiles() }
                button("Remove Selected") { removeSelectedContextFiles() }
            }
        }
        
        panel.preferredSize = java.awt.Dimension(600, 400)
        panel.minimumSize = java.awt.Dimension(400, 300)
        return panel
    }
    
    override fun doValidate(): ValidationInfo? {
        return when {
            nameField.text.isBlank() -> ValidationInfo("Name cannot be empty", nameField)
            promptTemplateArea.text.isBlank() -> ValidationInfo("Prompt template cannot be empty", promptTemplateArea)
            testPatternField.text.isBlank() -> ValidationInfo("Test file pattern cannot be empty", testPatternField)
            else -> null
        }
    }
    
    fun getTestType() = AiderProjectSettings.TestTypeConfiguration(
        name = nameField.text,
        promptTemplate = promptTemplateArea.text,
        referenceFilePattern = referencePatternField.text,
        testFilePattern = testPatternField.text,
        isEnabled = enabledCheckBox.isSelected,
        contextFiles = List(contextFilesModel.size()) { contextFilesModel.getElementAt(it) }
    )
}
