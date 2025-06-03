package de.andrena.codingaider.features.customactions.dialogs

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
import de.andrena.codingaider.features.customactions.CustomActionConfiguration
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

class CustomActionTypeDialog(
    private val project: Project,
    private val existing: CustomActionConfiguration?
) : DialogWrapper(project) {
    
    private val nameField = JBTextField()
    private val promptTemplateArea = JBTextArea().apply {
        rows = 5
        lineWrap = true
        wrapStyleWord = true
    }
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
        title = if (existing == null) "Add Custom Action" else "Edit Custom Action"
        contextFilesList.model = contextFilesModel
        
        existing?.let {
            nameField.text = it.name
            promptTemplateArea.text = it.promptTemplate
            enabledCheckBox.isSelected = it.isEnabled
            it.contextFiles.forEach { file -> contextFilesModel.addElement(file) }
        }
        init()
    }

    private fun addContextFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
        val files = FileChooser.chooseFiles(descriptor, project, null)
        val projectPath = project.basePath ?: ""
        
        files.forEach { virtualFile ->
            if (virtualFile.isDirectory) {
                // Recursively process directories
                addFilesFromDirectory(virtualFile, projectPath)
            } else {
                // Add individual file
                addSingleFile(virtualFile.path, projectPath)
            }
        }
    }
    
    private fun addFilesFromDirectory(directory: com.intellij.openapi.vfs.VirtualFile, projectPath: String) {
        directory.children.forEach { child ->
            if (child.isDirectory) {
                // Recursively process subdirectories
                addFilesFromDirectory(child, projectPath)
            } else {
                // Add file
                addSingleFile(child.path, projectPath)
            }
        }
    }
    
    private fun addSingleFile(filePath: String, projectPath: String) {
        // Store paths relative to project root
        val relativePath = try {
            val rootPath = File(projectPath).toPath()
            val path = File(filePath).toPath()
            rootPath.relativize(path).toString().replace('\\', '/')
        } catch (e: IllegalArgumentException) {
            // If the path can't be relativized (e.g., different drive on Windows), use absolute path
            filePath
        }
        
        if (!contextFilesModel.contains(relativePath)) {
            contextFilesModel.addElement(relativePath)
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
            else -> null
        }
    }
    
    fun getCustomAction() = CustomActionConfiguration(
        name = nameField.text,
        promptTemplate = promptTemplateArea.text,
        isEnabled = enabledCheckBox.isSelected,
        contextFiles = List(contextFilesModel.size()) { contextFilesModel.getElementAt(it) }
    )
}
