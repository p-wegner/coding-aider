package de.andrena.codingaider.features.documentation.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.treeStructure.Tree
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.features.documentation.DocumentTypeConfiguration
import de.andrena.codingaider.features.documentation.DocumentationGenerationPromptService
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.services.GitRepoCloneService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderProjectSettingsConfigurable
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.FileTraversal
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class GitRepoDocumentationDialog(
    private val project: Project
) : DialogWrapper(project) {
    
    private val settings = AiderProjectSettings.getInstance(project)
    private val gitService = project.service<GitRepoCloneService>()
    private val settingsButton = createSettingsButton()
    
    // Git Repository fields
    private val repoUrlField = JBTextField().apply {
        emptyText.text = "Enter Git repository URL (https://github.com/user/repo.git)"
    }
    
    private val branchComboBox = ComboBox<String>().apply {
        addItem("main")
        addItem("master")
        isEditable = true
    }
    
    private val cloneButton = JButton("Clone Repository").apply {
        addActionListener { cloneRepository() }
    }
    
    private val fileTree = Tree().apply {
        model = DefaultTreeModel(DefaultMutableTreeNode("No repository cloned"))
        isRootVisible = true
        showsRootHandles = true
    }
    
    // Documentation fields (reused from DocumentationGenerationDialog)
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
        addActionListener { updatePromptTemplate() }
    }
    
    private val filenameField = JBTextField().apply {
        emptyText.text = "Enter filename for documentation"
    }
    
    private val promptArea = JBTextArea().apply {
        rows = 10
        lineWrap = true
        wrapStyleWord = true
        font = font.deriveFont(12f)
        emptyText.text = "Enter additional instructions (optional)"
    }
    
    private var clonedRepoPath: String? = null
    private var selectedFiles: Array<VirtualFile> = emptyArray()

    init {
        title = "Generate Documentation from Git Repository"
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

    private fun cloneRepository() {
        val repoUrl = repoUrlField.text.trim()
        if (repoUrl.isEmpty()) {
            Messages.showErrorDialog("Please enter a repository URL", "Error")
            return
        }
        
        val selectedBranch = branchComboBox.selectedItem as? String
        
        cloneButton.isEnabled = false
        cloneButton.text = "Cloning..."
        
        gitService.cloneRepositoryAsync(repoUrl, selectedBranch).thenAccept { result ->
            SwingUtilities.invokeLater {
                cloneButton.isEnabled = true
                cloneButton.text = "Clone Repository"
                
                if (result.success && result.localPath != null) {
                    clonedRepoPath = result.localPath
                    updateFileTree(result.localPath)
                    updateBranchComboBox(result.branches)
                    Messages.showInfoMessage("Repository cloned successfully!", "Success")
                } else {
                    Messages.showErrorDialog(
                        "Failed to clone repository: ${result.error ?: "Unknown error"}",
                        "Clone Error"
                    )
                }
            }
        }
    }
    
    private fun updateFileTree(repoPath: String) {
        val root = DefaultMutableTreeNode(File(repoPath).name)
        val model = DefaultTreeModel(root)
        
        val repoDir = File(repoPath)
        if (repoDir.exists()) {
            addDirectoryToTree(root, repoDir)
        }
        
        fileTree.model = model
        fileTree.expandRow(0)
    }
    
    private fun addDirectoryToTree(parentNode: DefaultMutableTreeNode, directory: File) {
        directory.listFiles()?.forEach { file ->
            if (!file.name.startsWith(".")) { // Skip hidden files like .git
                val node = DefaultMutableTreeNode(file)
                parentNode.add(node)
                
                if (file.isDirectory) {
                    addDirectoryToTree(node, file)
                }
            }
        }
    }
    
    private fun updateBranchComboBox(branches: List<String>) {
        branchComboBox.removeAllItems()
        branches.forEach { branchComboBox.addItem(it) }
        if (branches.isEmpty()) {
            branchComboBox.addItem("main")
            branchComboBox.addItem("master")
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            group("Git Repository") {
                row {
                    label("Repository URL:")
                }
                row {
                    cell(repoUrlField)
                        .resizableColumn()
                        .align(AlignX.FILL)
                }
                row {
                    label("Branch/Tag:")
                    cell(branchComboBox)
                        .align(AlignX.LEFT)
                    cell(cloneButton)
                        .align(AlignX.RIGHT)
                }
                row {
                    label("Select files/folders:")
                }
                row {
                    cell(JBScrollPane(fileTree))
                        .resizableColumn()
                        .align(AlignY.FILL)
                        .align(AlignX.FILL)
                }.resizableRow()
            }
            
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
            }
        }
        
        panel.preferredSize = Dimension(900, 700)
        panel.minimumSize = Dimension(600, 500)
        return panel
    }

    override fun doValidate(): ValidationInfo? {
        val documentType = getSelectedDocumentType()
        return when {
            clonedRepoPath == null -> ValidationInfo("Please clone a repository first")
            documentType == null -> ValidationInfo("Please select a document type")
            settings.getDocumentTypes().isEmpty() -> ValidationInfo("No document types configured. Please configure document types in Project Settings.")
            filenameField.text.isBlank() -> ValidationInfo("Please enter a filename for the documentation", filenameField)
            getSelectedFiles().isEmpty() -> ValidationInfo("Please select at least one file or folder from the repository")
            else -> null
        }
    }

    override fun doOKAction() {
        val documentType = getSelectedDocumentType() ?: return
        val filename = filenameField.text
        val selectedFiles = getSelectedFiles()
        
        if (selectedFiles.isEmpty()) {
            Messages.showErrorDialog("Please select files or folders to document", "No Files Selected")
            return
        }
        
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
            IDEBasedExecutor(project, commandData) { success ->
                // Cleanup cloned repository after documentation generation
                clonedRepoPath?.let { gitService.cleanupRepository(it) }
            }.execute()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to generate documentation: ${e.message}",
                "Documentation Generation Error"
            )
        }
    }
    
    private fun getSelectedFiles(): Array<VirtualFile> {
        val selectedPaths = fileTree.selectionPaths ?: return emptyArray()
        val virtualFiles = mutableListOf<VirtualFile>()
        
        clonedRepoPath?.let { repoPath ->
            val repoRoot = gitService.getRepositoryRoot(repoPath)
            if (repoRoot != null) {
                selectedPaths.forEach { path ->
                    val node = path.lastPathComponent as? DefaultMutableTreeNode
                    val file = node?.userObject as? File
                    if (file != null) {
                        val relativePath = File(repoPath).toPath().relativize(file.toPath()).toString()
                        val virtualFile = repoRoot.findFileByRelativePath(relativePath)
                        if (virtualFile != null) {
                            virtualFiles.add(virtualFile)
                        }
                    }
                }
            }
        }
        
        return virtualFiles.toTypedArray()
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
    
    override fun doCancelAction() {
        // Cleanup cloned repository if user cancels
        clonedRepoPath?.let { gitService.cleanupRepository(it) }
        super.doCancelAction()
    }
}
