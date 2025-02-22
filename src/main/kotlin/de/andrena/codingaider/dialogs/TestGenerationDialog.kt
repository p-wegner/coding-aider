package de.andrena.codingaider.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.TestTypeConfiguration
import javax.swing.JComponent
import javax.swing.JPanel

class TestGenerationDialog(
    private val project: Project,
    private val selectedFiles: List<String>
) : DialogWrapper(project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private val testTypeComboBox = ComboBox<TestTypeConfiguration>()
    private val promptArea = JBTextArea().apply {
        rows = 5
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        title = "Generate Tests"
        init()
        updateTestTypes()
    }

    private fun updateTestTypes() {
        testTypeComboBox.removeAllItems()
        settings.getTestTypes()
            .filter { it.isEnabled }
            .forEach { testTypeComboBox.addItem(it) }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Test Type:") {
                cell(testTypeComboBox)
            }
            row("Additional Instructions:") {
                cell(JBScrollPane(promptArea))
                    .resizableColumn()
                    .rows(2)
            }
        }
    }

    fun getSelectedTestType(): TestTypeConfiguration? = testTypeComboBox.selectedItem as? TestTypeConfiguration
    fun getAdditionalPrompt(): String = promptArea.text
}
