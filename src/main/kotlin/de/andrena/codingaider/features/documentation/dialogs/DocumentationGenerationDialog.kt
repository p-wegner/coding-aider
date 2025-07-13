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
import de.andrena.codingaider.services.AiderIgnoreService
import de.andrena.codingaider.services.TokenCountService
import java.awt.Component
import java.awt.Dimension
import java.io.File
import java.text.NumberFormat
import java.util.concurrent.CompletableFuture
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class DocumentationGenerationDialog(
    private val project: Project,
    private val selectedFiles: Array<VirtualFile>
) : DialogWrapper(project) {
    private val settings = AiderProjectSettings.getInstance(project)
    private val aiderIgnoreService = project.service<AiderIgnoreService>()
    private val tokenCountService = project.service<TokenCountService>()
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
    
    private val fileCountLabel = JLabel("Files: Calculating...")
    private val tokenCountLabel = JLabel("Tokens: Calculating...")
    private var filteredFiles: List<FileData> = emptyList()
    private var tokenCountFuture: CompletableFuture<Int>? = null

    init {
        title = "Generate Documentation"
        init()
        updateDocumentTypes()
        updateFilePresentation()
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
                label("Files to be documented:")
            }
            row {
                cell(fileCountLabel)
                cell(tokenCountLabel)
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
        
        return when {
            documentType == null -> ValidationInfo("Please select a document type")
            settings.getDocumentTypes().isEmpty() -> ValidationInfo("No document types configured. Please configure document types in Project Settings.")
            else -> null
        }
    }

    override fun doOKAction() {
        val documentType = getSelectedDocumentType() ?: return
        val filename = filenameField.text
        
        // Cancel any ongoing token counting
        tokenCountFuture?.cancel(true)
        
        val settings = AiderSettings.getInstance()

        try {
            // Add context files to the file list - convert relative paths to absolute
            val absoluteDocumentType = documentType.withAbsolutePaths(project.basePath ?: "")
            val contextFiles = absoluteDocumentType.contextFiles.map { FileData(it, false) }
        
            val commandData = CommandData(
                message = buildPrompt(documentType, filteredFiles, filename),
                useYesFlag = true,
                files = filteredFiles + contextFiles,
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

    private fun updateFilePresentation() {
        // Collect all files and filter out ignored ones
        val allFiles = project.service<FileDataCollectionService>().collectAllFiles(selectedFiles)
        filteredFiles = allFiles.filter { !aiderIgnoreService.isIgnored(it.filePath) }
        
        // Update file count immediately
        val formatter = NumberFormat.getNumberInstance()
        fileCountLabel.text = "Files: ${formatter.format(filteredFiles.size)}"
        
        // Cancel any previous token counting
        tokenCountFuture?.cancel(true)
        
        // Start async token counting
        tokenCountLabel.text = "Tokens: Calculating..."
        tokenCountFuture = tokenCountService.countTokensInFilesAsync(filteredFiles)
        
        tokenCountFuture?.thenAccept { tokenCount ->
            SwingUtilities.invokeLater {
                if (!isDisposed) {
                    tokenCountLabel.text = "Tokens: ${formatter.format(tokenCount)}"
                }
            }
        }?.exceptionally { throwable ->
            SwingUtilities.invokeLater {
                if (!isDisposed) {
                    tokenCountLabel.text = "Tokens: Error calculating"
                }
            }
            null
        }
    }
    
    override fun dispose() {
        tokenCountFuture?.cancel(true)
        super.dispose()
    }

    private fun getSelectedDocumentType(): DocumentTypeConfiguration? = documentTypeComboBox.selectedItem as? DocumentTypeConfiguration
    private fun getAdditionalPrompt(): String = promptArea.text
    
}
