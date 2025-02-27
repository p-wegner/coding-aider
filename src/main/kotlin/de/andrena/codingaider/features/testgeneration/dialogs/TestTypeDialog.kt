package de.andrena.codingaider.features.testgeneration.dialogs

import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.features.testgeneration.TestGenerationPromptService
import de.andrena.codingaider.features.testgeneration.TestTypeConfiguration
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
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
    
    private fun evaluateWithAider() {
        val testType = getTestType()
        val projectPath = project.basePath ?: ""
        
        try {
            val promptService = project.service<TestGenerationPromptService>()
            val prompt = promptService.buildTestAbstractionPrompt(testType )
            
            val commandData = CommandData(
                message = prompt,
                useYesFlag = AiderSettings.getInstance().useYesFlag,
                files = testType.withAbsolutePaths(projectPath).contextFiles.map { FileData(it, false) },
                projectPath = projectPath,
                llm = AiderSettings.getInstance().llm,
                additionalArgs = AiderSettings.getInstance().additionalArgs,
                lintCmd = AiderSettings.getInstance().lintCmd,
                aiderMode = AiderMode.NORMAL,
                sidecarMode = AiderSettings.getInstance().useSidecarMode,
            )
            
            IDEBasedExecutor(project, commandData).execute()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to evaluate with Aider: ${e.message}",
                "Evaluation Error"
            )
        }
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
                button("Abstract Test Strategy Using AI") { evaluateWithAider() }
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
