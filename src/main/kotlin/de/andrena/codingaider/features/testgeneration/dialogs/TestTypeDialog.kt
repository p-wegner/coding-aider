package de.andrena.codingaider.features.testgeneration.dialogs

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.features.testgeneration.TestTypeConfiguration
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

class TestTypeDialog(
    private val project: Project,
    private val existing: TestTypeConfiguration?
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
    private val contextFilesList = JBList<String>().apply {
        cellRenderer = ContextFileRenderer()
    }
    private inner class ContextFileRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is String) {
                val file = File(value)
                component.text = file.name
                component.toolTipText = value
                
                // Show a visual indicator for relative vs absolute paths
                if (!file.isAbsolute) {
                    component.text = "${file.name} (relative)"
                } else {
                    component.text = "${file.name} (absolute)"
                }
            }
            return component
        }
    }

    private val contextFilesModel = DefaultListModel<String>()
    
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
        val projectPath = project.basePath ?: ""
        
        files.forEach { file ->
            // Store paths relative to project root
            val relativePath = try {
                val rootPath = File(projectPath).toPath()
                val filePath = File(file.path).toPath()
                rootPath.relativize(filePath).toString().replace('\\', '/')
            } catch (e: IllegalArgumentException) {
                // If the path can't be relativized (e.g., different drive on Windows), use absolute path
                file.path
            }
            
            if (!contextFilesModel.contains(relativePath)) {
                contextFilesModel.addElement(relativePath)
            }
        }
    }

    private fun removeSelectedContextFiles() {
        contextFilesList.selectedValuesList.forEach { contextFilesModel.removeElement(it) }
    }
    
    override fun createCenterPanel(): JComponent {
        val contentPanel = panel {
            row("Name:") {
                cell(nameField)
                    .resizableColumn()
                    .align(AlignX.FILL)
                cell(enabledCheckBox)
                    .align(AlignX.RIGHT)
            }
            row("Prompt Template:") {
                cell(JBScrollPane(promptTemplateArea))
                    .resizableColumn()
                    .align(AlignY.FILL)
                    .align(AlignX.FILL)
            }.resizableRow()
            row("Reference File Pattern:") {
                cell(referencePatternField)
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
            row("Test File Pattern:") {
                cell(testPatternField)
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
            row("Context Files:") {
                cell(JBScrollPane(contextFilesList))
                    .resizableColumn()
                    .align(AlignY.FILL)
                    .align(AlignX.FILL)
            }.resizableRow()
            row {
                button("Add Files") { addContextFiles() }
                button("Remove Selected") { removeSelectedContextFiles() }
            }
        }
        
        contentPanel.preferredSize = Dimension(800, 600)
        
        return JBScrollPane(contentPanel).apply {
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            border = null // Remove border for cleaner look
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
    
    fun getTestType() = TestTypeConfiguration(
        name = nameField.text,
        promptTemplate = promptTemplateArea.text,
        referenceFilePattern = referencePatternField.text,
        testFilePattern = testPatternField.text,
        isEnabled = enabledCheckBox.isSelected,
        contextFiles = List(contextFilesModel.size()) { contextFilesModel.getElementAt(it) }
    )
}
