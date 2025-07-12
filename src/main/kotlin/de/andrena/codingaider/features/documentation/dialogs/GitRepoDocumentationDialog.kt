package de.andrena.codingaider.features.documentation.dialogs

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.*
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.features.documentation.DocumentTypeConfiguration
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.services.GitRepoCloneService
import de.andrena.codingaider.settings.AiderSettings
import java.awt.Dimension
import java.io.File
import javax.swing.*

class GitRepoDocumentationDialog(
    private val project: Project
) : DialogWrapper(project) {
    private val logger = Logger.getInstance(GitRepoDocumentationDialog::class.java)
    private val gitService = project.service<GitRepoCloneService>()
    
    // UI Panels
    private val gitRepositoryPanel = GitRepositoryPanel(project)
    private val fileSelectionPanel = FileSelectionPanel(project)
    private val documentationConfigPanel = DocumentationConfigPanel(project)
    
    private var clonedRepoPath: String? = null
    private var selectedFiles: Array<VirtualFile> = emptyArray()

    // Constructor for pre-selected files
    constructor(project: Project, preSelectedFiles: Array<VirtualFile>, repoPath: String) : this(project) {
        this.selectedFiles = preSelectedFiles
        this.clonedRepoPath = repoPath
        // Initialize UI with pre-selected data
        SwingUtilities.invokeLater {
            fileSelectionPanel.updateFileTreeFromVirtualFiles(preSelectedFiles, repoPath)
            updateRepositoryInfoFromPath(repoPath)
        }
    }

    init {
        title = "Generate Documentation from Git Repository"
        init()
        setupCallbacks()
    }
    
    private fun setupCallbacks() {
        // Git repository panel callbacks
        gitRepositoryPanel.onCloneSuccess = { result ->
            if (result.success && result.repoPath != null) {
                clonedRepoPath = result.repoPath
                fileSelectionPanel.updateFileTree(result.repoPath)
                
                // Auto-populate filename if empty
                if (documentationConfigPanel.filenameField.text.isBlank()) {
                    val repoName = gitRepositoryPanel.repoUrlField.text.substringAfterLast("/").removeSuffix(".git")
                    documentationConfigPanel.filenameField.text = "${repoName}_documentation.md"
                }
                
                // Enable OK button
                setOKActionEnabled(true)
            }
        }
        
        gitRepositoryPanel.onBranchTagSwitch = { item ->
            val repoPath = clonedRepoPath
            if (repoPath != null) {
                val success = gitRepositoryPanel.switchBranchOrTag(item)
                if (success) {
                    // Refresh file tree after branch/tag switch
                    fileSelectionPanel.updateFileTree(repoPath)
                }
            }
        }
    }

    fun setPreSelectedFiles(files: Array<VirtualFile>, repoPath: String) {
        this.selectedFiles = files
        this.clonedRepoPath = repoPath
        SwingUtilities.invokeLater {
            fileSelectionPanel.updateFileTreeFromVirtualFiles(files, repoPath)
            updateRepositoryInfoFromPath(repoPath)
            // Hide Git repository section since we already have the files
            gitRepositoryPanel.hide()
            repaint()
        }
    }

    private fun updateRepositoryInfoFromPath(repoPath: String) {
        val repoName = File(repoPath).name
        val fileCount = selectedFiles.size
        
        // Also populate some default filename if empty
        if (documentationConfigPanel.filenameField.text.isBlank()) {
            documentationConfigPanel.filenameField.text = "${repoName}_documentation.md"
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                cell(gitRepositoryPanel.createPanel())
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
            row {
                cell(fileSelectionPanel.createPanel())
                    .resizableColumn()
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
            }.resizableRow()
            row {
                cell(documentationConfigPanel.createPanel())
                    .resizableColumn()
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
            }.resizableRow()
        }

        panel.preferredSize = Dimension(900, 700)
        panel.minimumSize = Dimension(600, 500)
        return panel
    }


    override fun doValidate(): ValidationInfo? {
        val documentType = documentationConfigPanel.getSelectedDocumentType()
        val repoUrl = gitRepositoryPanel.repoUrlField.text.trim()
        val filename = documentationConfigPanel.filenameField.text.trim()

        return when {
            // Only validate repo URL if it's not empty (for cases where repo is already cloned)
            repoUrl.isNotEmpty() && !gitRepositoryPanel.validateRepositoryUrl() -> ValidationInfo(
                "Invalid repository URL format",
                gitRepositoryPanel.repoUrlField
            )
            // Allow validation to pass if we have a cloned repo path (pre-selected scenario)
            clonedRepoPath == null && repoUrl.isEmpty() && selectedFiles.isEmpty() -> ValidationInfo("Please clone a repository first or enter a repository URL")
            documentType == null -> ValidationInfo("Please select a document type", documentationConfigPanel.documentTypeComboBox)
            filename.isBlank() -> ValidationInfo("Please enter a filename for the documentation", documentationConfigPanel.filenameField)
            filename.isNotBlank() && !isValidFilename(filename) -> ValidationInfo(
                "Invalid filename. Please use only valid filename characters.",
                documentationConfigPanel.filenameField
            )
            documentType?.promptTemplate?.isBlank() == true -> ValidationInfo(
                "Selected document type has no prompt template configured",
                documentationConfigPanel.documentTypeComboBox
            )
            selectedFiles.isEmpty() && getSelectedFiles().isEmpty() -> ValidationInfo("Please select at least one file or folder from the repository")
            else -> null
        }
    }

    private fun isValidFilename(filename: String): Boolean {
        if (filename.isBlank()) return false
        val invalidChars = charArrayOf('<', '>', ':', '"', '|', '?', '*', '\\', '/')
        return !filename.any { it in invalidChars } && filename.trim() == filename
    }

    override fun doOKAction() {
        val documentType = documentationConfigPanel.getSelectedDocumentType() ?: return
        val filename = documentationConfigPanel.filenameField.text.trim()

        // Validate inputs before proceeding
        if (filename.isEmpty()) {
            Messages.showErrorDialog("Please enter a filename for the documentation", "Validation Error")
            return
        }

        // Use pre-selected files if available, otherwise get from tree
        val filesToDocument = if (selectedFiles.isNotEmpty()) {
            selectedFiles
        } else {
            getSelectedFiles()
        }

        if (filesToDocument.isEmpty()) {
            Messages.showErrorDialog("Please select files or folders to document", "No Files Selected")
            return
        }

        // Validate document type configuration
        if (documentType.promptTemplate.isBlank()) {
            Messages.showErrorDialog(
                "The selected document type has no prompt template configured. Please configure it in Project Settings.",
                "Configuration Error"
            )
            return
        }

        // Validate repository state before proceeding
        if (!validateRepositoryState()) {
            return
        }

        // Show confirmation dialog before proceeding
        if (!showConfirmationDialog(filesToDocument, documentType, filename)) {
            return
        }

        val allFiles = project.service<FileDataCollectionService>().collectAllFiles(filesToDocument, false)
        val globalSettings = AiderSettings.getInstance()

        try {
            // Add context files to the file list - convert relative paths to absolute
            val absoluteDocumentType = documentType.withAbsolutePaths(project.basePath ?: "")
            val contextFiles = absoluteDocumentType.contextFiles.mapNotNull { contextPath ->
                val contextFile = File(contextPath)
                if (contextFile.exists()) {
                    FileData(contextPath, false)
                } else {
                    null // Skip non-existent context files
                }
            }

            val commandData = CommandData(
                message = documentationConfigPanel.buildPrompt(documentType, allFiles, filename),
                useYesFlag = true,
                files = allFiles + contextFiles,
                projectPath = project.basePath ?: "",
                llm = globalSettings.llm,
                additionalArgs = globalSettings.additionalArgs,
                lintCmd = globalSettings.lintCmd,
                aiderMode = AiderMode.NORMAL,
                sidecarMode = globalSettings.useSidecarMode,
            )

            // Show progress notification
            val notificationGroup = com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Aider Web Crawl")

            val startNotification = notificationGroup.createNotification(
                "Documentation Generation Started",
                "Generating documentation for ${allFiles.size} files using ${documentType.name}...",
                com.intellij.notification.NotificationType.INFORMATION
            )
            startNotification.notify(project)

            // Execute documentation generation with proper callback and progress updates
            val executor = IDEBasedExecutor(project, commandData) { success ->
                SwingUtilities.invokeLater {
                    // Cleanup cloned repository after documentation generation
                    clonedRepoPath?.let {
                        gitService.cleanupRepository(it)
                    }

                    // Show completion notification
                    val completionNotification = if (success) {
                        notificationGroup.createNotification(
                            "Documentation Generation Completed",
                            "Documentation has been successfully generated as '$filename'",
                            com.intellij.notification.NotificationType.INFORMATION
                        )
                    } else {
                        notificationGroup.createNotification(
                            "Documentation Generation Failed",
                            "There was an error generating the documentation. Please check the output for details.",
                            com.intellij.notification.NotificationType.ERROR
                        )
                    }
                    completionNotification.notify(project)
                }
            }

            // Show progress during execution
            executor.execute()

            // Only close dialog after successfully starting documentation generation
            super.doOKAction()

        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to generate documentation: ${e.message}",
                "Documentation Generation Error"
            )
            // Don't close dialog on error
        }
    }

    private fun getSelectedFiles(): Array<VirtualFile> {
        return clonedRepoPath?.let { repoPath ->
            fileSelectionPanel.getSelectedFiles(repoPath)
        } ?: emptyArray()
    }

    private fun validateRepositoryState(): Boolean {
        val repoPath = clonedRepoPath
        if (repoPath != null) {
            val repoDir = File(repoPath)
            if (!repoDir.exists()) {
                Messages.showErrorDialog(
                    "The cloned repository directory no longer exists. Please clone the repository again.",
                    "Repository State Error"
                )
                return false
            }

            // Check if .git directory exists
            val gitDir = File(repoDir, ".git")
            if (!gitDir.exists()) {
                Messages.showErrorDialog(
                    "The repository appears to be corrupted (missing .git directory). Please clone again.",
                    "Repository State Error"
                )
                return false
            }
        }
        return true
    }

    private fun showConfirmationDialog(
        filesToDocument: Array<VirtualFile>,
        documentType: DocumentTypeConfiguration,
        filename: String
    ): Boolean {
        val fileCount = filesToDocument.size
        val totalSize = filesToDocument.sumOf { it.length }
        val sizeText = when {
            totalSize < 1024 -> "$totalSize bytes"
            totalSize < 1024 * 1024 -> "${totalSize / 1024} KB"
            else -> "${totalSize / (1024 * 1024)} MB"
        }

        val message = buildString {
            appendLine("Ready to generate documentation with the following settings:")
            appendLine()
            appendLine("📄 Document Type: ${documentType.name}")
            appendLine("📁 Files to process: $fileCount files")
            appendLine("📊 Total size: $sizeText")
            appendLine("💾 Output filename: $filename")
            appendLine()
            if (documentType.contextFiles.isNotEmpty()) {
                appendLine("📋 Context files: ${documentType.contextFiles.size} additional files")
                appendLine()
            }
            appendLine("This will:")
            appendLine("• Analyze all selected files")
            appendLine("• Generate comprehensive documentation")
            appendLine("• Save the result as '$filename'")
            if (clonedRepoPath != null) {
                appendLine("• Clean up the temporary repository")
            }
            appendLine()
            appendLine("Do you want to proceed?")
        }

        val result = Messages.showYesNoDialog(
            project,
            message,
            "Confirm Documentation Generation",
            "Generate Documentation",
            "Cancel",
            Messages.getQuestionIcon()
        )

        return result == Messages.YES
    }

    override fun doCancelAction() {
        // Cleanup cloned repository if user cancels
        gitRepositoryPanel.cleanup()
        super.doCancelAction()
    }
}
