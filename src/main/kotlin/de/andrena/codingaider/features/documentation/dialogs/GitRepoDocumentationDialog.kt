package de.andrena.codingaider.features.documentation.dialogs

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.dsl.builder.*
import de.andrena.codingaider.services.GitRepoCloneService
import java.awt.Dimension
import java.io.File
import javax.swing.*

/**
 * Simplified dialog for Git repository operations only.
 * After successful cloning, this opens the standard DocumentationGenerationDialog.
 */
class GitRepoDocumentationDialog(
    private val project: Project
) : DialogWrapper(project) {
    private val logger = Logger.getInstance(GitRepoDocumentationDialog::class.java)
    private val gitService = project.service<GitRepoCloneService>()
    
    // UI Panels
    private val gitRepositoryPanel = GitRepositoryPanel(project)
    private val fileSelectionPanel = FileSelectionPanel(project)
    
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
        title = "Clone Git Repository for Documentation"
        init()
        setupCallbacks()
    }
    
    private fun setupCallbacks() {
        // Git repository panel callbacks
        gitRepositoryPanel.onCloneSuccess = { result ->
            if (result.success && result.repoPath != null) {
                clonedRepoPath = result.repoPath
                fileSelectionPanel.updateFileTree(result.repoPath)
                
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
        }

        panel.preferredSize = Dimension(900, 500)
        panel.minimumSize = Dimension(600, 400)
        return panel
    }

    override fun doValidate(): ValidationInfo? {
        val repoUrl = gitRepositoryPanel.repoUrlField.text.trim()

        return when {
            // Only validate repo URL if it's not empty (for cases where repo is already cloned)
            repoUrl.isNotEmpty() && !gitRepositoryPanel.validateRepositoryUrl() -> ValidationInfo(
                "Invalid repository URL format",
                gitRepositoryPanel.repoUrlField
            )
            // Allow validation to pass if we have a cloned repo path (pre-selected scenario)
            clonedRepoPath == null && repoUrl.isEmpty() && selectedFiles.isEmpty() -> ValidationInfo("Please clone a repository first or enter a repository URL")
            selectedFiles.isEmpty() && getSelectedFiles().isEmpty() -> ValidationInfo("Please select at least one file or folder from the repository")
            else -> null
        }
    }

    override fun doOKAction() {
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

        // Validate repository state before proceeding
        if (!validateRepositoryState()) {
            return
        }

        // Close this dialog and open the standard documentation generation dialog
        super.doOKAction()
        
        // Open DocumentationGenerationDialog with pre-selected files
        val documentationDialog = DocumentationGenerationDialog(project, filesToDocument)
        documentationDialog.show()
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

    override fun doCancelAction() {
        // Cleanup cloned repository if user cancels
        gitRepositoryPanel.cleanup()
        super.doCancelAction()
    }
}
