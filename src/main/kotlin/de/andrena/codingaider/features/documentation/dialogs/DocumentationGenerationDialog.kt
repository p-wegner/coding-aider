package de.andrena.codingaider.features.documentation.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderProjectSettingsConfigurable
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.features.documentation.DocumentationGenerationPromptService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.features.documentation.DocumentTypeConfiguration
import de.andrena.codingaider.services.FileDataCollectionService
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

class DocumentationGenerationDialog(
    private val project: Project,
    private val selectedFiles: Array<VirtualFile>
) : DialogWrapper(project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private val settingsButton = createSettingsButton()
    private val documentTypeComboBox = ComboBox<DocumentTypeConfiguration>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (component is JLabel && value is DocumentTypeConfiguration) {
                    text = value.name
                }
                return component
            }
        }
        addActionListener {
            updatePromptTemplate()
        }
    }
    
    private val filenameField = JBTextField().apply {
        emptyText.text = "Enter filename for documentation"
    }
    
    private val promptArea = JBTextArea().apply {
        rows = 15
        lineWrap = true
        wrapStyleWord = true
        font = font.deriveFont(12f)
        emptyText.text = "Enter additional instructions (optional)"
    }

    init {
        title = "Generate Documentation"
        init()
        updateDocumentTypes()
    }

    private fun updateDocumentTypes() {
        documentTypeComboBox.removeAllItems()
        settings.getDocumentTypes()
            .filter { it.isEnabled }
            .forEach { documentTypeComboBox.addItem(it) }
    }

    private fun updatePromptTemplate() {
        val selectedType = getSelectedDocumentType()
        if (selectedType != null) {
            val ellipsedTemplate = selectedType.promptTemplate.let { template ->
                if (template.length > 100) {
                    template.take(97) + "..."
                } else {
                    template
                }
            }
            promptArea.emptyText.text = ellipsedTemplate
        }
    }

    private fun createSettingsButton(): ActionButton {
        val settingsAction = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AiderProjectSettingsConfigurable::class.java)
            }
        }
        val presentation = Presentation("Open Settings").apply {
            icon = AllIcons.General.Settings
            description = "Open document type settings"
        }
        return ActionButton(
            settingsAction, presentation, "DocumentTypeSettingsButton", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
        )
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                label("Select the type of documentation to generate:")
            }
            row {
                cell(documentTypeComboBox)
                    .resizableColumn()
                    .align(AlignX.FILL)
                cell(settingsButton)
                    .align(AlignX.RIGHT)
            }
            row {
                label("Enter filename for documentation:")
            }
            row {
                cell(filenameField)
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
            row {
                label("Enter any additional instructions or requirements:")
            }
            row {
                cell(JBScrollPane(promptArea))
                    .resizableColumn()
                    .align(AlignY.FILL)
                    .align(AlignX.FILL)
            }.resizableRow()
        }
        
        panel.preferredSize = Dimension(800, 600)
        panel.minimumSize = Dimension(400, 300)
        return panel
    }

    override fun doValidate(): ValidationInfo? {
        val documentType = getSelectedDocumentType()
        val filename = filenameField.text.trim()
        
        return when {
            documentType == null -> ValidationInfo("Please select a document type")
            settings.getDocumentTypes().isEmpty() -> ValidationInfo("No document types configured. Please configure document types in Project Settings.")
            filename.isBlank() -> ValidationInfo("Please enter a filename for the documentation", filenameField)
            !isValidFilename(filename) -> ValidationInfo("Please enter a valid filename (avoid special characters like < > : \" | ? * \\)", filenameField)
            else -> null
        }
    }

    override fun doOKAction() {
        val documentType = getSelectedDocumentType() ?: return
        val filename = filenameField.text
        
        val allFiles = project.service<FileDataCollectionService>().collectAllFiles(selectedFiles)
        val settings = AiderSettings.getInstance()

        try {
            // Add context files to the file list - convert relative paths to absolute
            val absoluteDocumentType = documentType.withAbsolutePaths(project.basePath ?: "")
            val contextFiles = absoluteDocumentType.contextFiles.map { FileData(it, false) }
        
            val commandData = CommandData(
                message = buildPrompt(documentType, allFiles, filename),
                useYesFlag = true,
                files = allFiles + contextFiles,
                projectPath = project.basePath ?: "",
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                lintCmd = settings.lintCmd,
                aiderMode = AiderMode.NORMAL,
                sidecarMode = settings.useSidecarMode,
            )

            super.doOKAction()
            IDEBasedExecutor(project, commandData).execute()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to generate documentation: ${e.message}",
                "Documentation Generation Error"
            )
        }
    }

    private fun buildPrompt(documentType: DocumentTypeConfiguration, files: List<FileData>, filename: String): String {
        return project.service<DocumentationGenerationPromptService>().buildPrompt(
            documentType, 
            files, 
            filename,
            getAdditionalPrompt()
        )
    }

    private fun getSelectedDocumentType(): DocumentTypeConfiguration? = documentTypeComboBox.selectedItem as? DocumentTypeConfiguration
    private fun getAdditionalPrompt(): String = promptArea.text
    
    private fun isValidFilename(filename: String): Boolean {
        if (filename.isBlank()) return false
        
        // Check for invalid characters in Windows/Unix filenames
        val invalidChars = setOf('<', '>', ':', '"', '|', '?', '*', '\\')
        if (filename.any { it in invalidChars }) return false
        
        // Check for reserved names on Windows
        val reservedNames = setOf("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9")
        val nameWithoutExtension = filename.substringBeforeLast('.').uppercase()
        if (nameWithoutExtension in reservedNames) return false
        
        // Check for names ending with space or dot
        if (filename.endsWith(' ') || filename.endsWith('.')) return false
        
        return true
    }
}
