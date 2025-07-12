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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.features.documentation.DocumentTypeConfiguration
import de.andrena.codingaider.features.documentation.DocumentationGenerationPromptService
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderProjectSettingsConfigurable
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList

class DocumentationConfigPanel(private val project: Project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private val settingsButton = createSettingsButton()
    
    val documentTypeComboBox = ComboBox<DocumentTypeConfiguration>().apply {
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
        addActionListener { updatePromptTemplate() }
    }
    
    val filenameField = JBTextField().apply {
        emptyText.text = "Enter filename for documentation"
    }
    
    val promptArea = JBTextArea().apply {
        rows = 10
        lineWrap = true
        wrapStyleWord = true
        font = font.deriveFont(12f)
        emptyText.text = "Enter additional instructions (optional)"
    }
    
    init {
        updateDocumentTypes()
    }
    
    fun createPanel() = panel {
        group("Documentation Generation") {
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
            row {
                button("Preview Documentation Prompt") {
                    showDocumentationPreview()
                }.align(AlignX.CENTER)
            }
        }
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
    
    fun getSelectedDocumentType(): DocumentTypeConfiguration? =
        documentTypeComboBox.selectedItem as? DocumentTypeConfiguration
    
    fun getAdditionalPrompt(): String = promptArea.text
    
    fun buildPrompt(documentType: DocumentTypeConfiguration, files: List<FileData>, filename: String): String {
        return project.service<DocumentationGenerationPromptService>().buildPrompt(
            documentType,
            files,
            filename,
            getAdditionalPrompt()
        )
    }
    
    private fun showDocumentationPreview(): Boolean {
        val documentType = getSelectedDocumentType() ?: return false
        val filename = filenameField.text.trim()

        if (filename.isEmpty()) {
            Messages.showErrorDialog("Please enter a filename first", "Missing Filename")
            return false
        }

        // This would need to be called from the main dialog with actual files
        Messages.showInfoMessage("Preview functionality requires selected files from the main dialog", "Preview")
        return true
    }
    
    fun showDocumentationPreview(filesToDocument: Array<VirtualFile>): Boolean {
        val documentType = getSelectedDocumentType() ?: return false
        val filename = filenameField.text.trim()

        if (filesToDocument.isEmpty()) return false

        val allFiles = project.service<FileDataCollectionService>().collectAllFiles(filesToDocument, false)
        val previewPrompt = buildPrompt(documentType, allFiles, filename)

        val previewDialog = object : DialogWrapper(project) {
            init {
                title = "Documentation Preview"
                init()
            }

            override fun createCenterPanel() = panel {
                row {
                    label("This is the prompt that will be sent to generate the documentation:")
                }
                row {
                    cell(JBScrollPane(JBTextArea(previewPrompt).apply {
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                        font = font.deriveFont(12f)
                    }))
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .align(AlignY.FILL)
                }.resizableRow()
            }

            override fun createActions() = arrayOf(okAction, cancelAction)
        }

        return previewDialog.showAndGet()
    }
}
