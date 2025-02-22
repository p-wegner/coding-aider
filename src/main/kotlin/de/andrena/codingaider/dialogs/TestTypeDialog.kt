package de.andrena.codingaider.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import de.andrena.codingaider.settings.AiderProjectSettings
import javax.swing.JComponent

class TestTypeDialog(
    project: Project,
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
    
    init {
        title = if (existing == null) "Add Test Type" else "Edit Test Type"
        existing?.let {
            nameField.text = it.name
            promptTemplateArea.text = it.promptTemplate
            referencePatternField.text = it.referenceFilePattern
            testPatternField.text = it.testFilePattern
            enabledCheckBox.isSelected = it.isEnabled
        }
        init()
    }
    
    override fun createCenterPanel(): JComponent = panel {
        row("Name:") {
            cell(nameField)
                .resizableColumn()
        }
        row("Prompt Template:") {
            cell(JBScrollPane(promptTemplateArea))
                .resizableColumn()
        }
        row("Reference File Pattern:") {
            cell(referencePatternField)
                .resizableColumn()
        }
        row("Test File Pattern:") {
            cell(testPatternField)
                .resizableColumn()
        }
        row {
            cell(enabledCheckBox)
        }
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
        isEnabled = enabledCheckBox.isSelected
    )
}
