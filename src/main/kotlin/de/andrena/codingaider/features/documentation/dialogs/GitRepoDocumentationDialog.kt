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
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
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
        addActionListener { validateRepositoryUrl() }
    }
    
    private val branchComboBox = ComboBox<String>().apply {
        addItem("main")
        addItem("master")
        isEditable = true
    }
    
    private val cloneButton = JButton("Clone Repository").apply {
        addActionListener { cloneRepository() }
    }
    
    private val repoInfoLabel = JLabel("").apply {
        isVisible = false
    }
    
    private val fileTree = CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree?,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            if (value is CheckedTreeNode) {
                val userObject = value.userObject
                if (userObject is File) {
                    textRenderer.append(userObject.name)
                    if (userObject.isDirectory) {
                        textRenderer.icon = AllIcons.Nodes.Folder
                    } else {
                        textRenderer.icon = AllIcons.FileTypes.Text
                    }
                } else {
                    textRenderer.append(userObject?.toString() ?: "")
                }
            }
        }
    }, CheckedTreeNode("No repository cloned")).apply {
        isRootVisible = true
        showsRootHandles = true
    }
    
    private val selectAllButton = JButton("Select All").apply {
        addActionListener { selectAllFiles(true) }
    }
    
    private val deselectAllButton = JButton("Deselect All").apply {
        addActionListener { selectAllFiles(false) }
    }
    
    private val fileTypeFilterField = JBTextField().apply {
        emptyText.text = "Filter by file extensions (e.g., .kt,.java,.md)"
        addActionListener { applyFileTypeFilter() }
    }
    
    private val applyFilterButton = JButton("Apply Filter").apply {
        addActionListener { applyFileTypeFilter() }
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

    private fun validateRepositoryUrl(): Boolean {
        val repoUrl = repoUrlField.text.trim()
        if (repoUrl.isEmpty()) return false
        
        val validPatterns = listOf(
            Regex("^https://github\\.com/[\\w.-]+/[\\w.-]+(\\.git)?/?$"),
            Regex("^git@github\\.com:[\\w.-]+/[\\w.-]+\\.git$"),
            Regex("^https://gitlab\\.com/[\\w.-]+/[\\w.-]+(\\.git)?/?$"),
            Regex("^git@gitlab\\.com:[\\w.-]+/[\\w.-]+\\.git$"),
            Regex("^https://bitbucket\\.org/[\\w.-]+/[\\w.-]+(\\.git)?/?$"),
            Regex("^git@bitbucket\\.org:[\\w.-]+/[\\w.-]+\\.git$"),
            Regex("^https://[\\w.-]+/[\\w.-]+/[\\w.-]+(\\.git)?/?$") // Generic Git hosting
        )
        
        return validPatterns.any { it.matches(repoUrl) }
    }
    
    private fun cloneRepository() {
        val repoUrl = repoUrlField.text.trim()
        if (repoUrl.isEmpty()) {
            Messages.showErrorDialog("Please enter a repository URL", "Error")
            return
        }
        
        if (!validateRepositoryUrl()) {
            Messages.showErrorDialog(
                "Invalid repository URL format. Please enter a valid Git repository URL.\n" +
                "Examples:\n" +
                "• https://github.com/user/repo.git\n" +
                "• git@github.com:user/repo.git\n" +
                "• https://gitlab.com/user/repo.git",
                "Invalid URL"
            )
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
                    updateRepositoryInfo(repoUrl, result.branches.size, result.tags.size)
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
        val root = CheckedTreeNode(File(repoPath).name)
        root.userObject = File(repoPath)
        
        val repoDir = File(repoPath)
        if (repoDir.exists()) {
            addDirectoryToTree(root, repoDir)
        }
        
        fileTree.model = DefaultTreeModel(root)
        fileTree.expandRow(0)
    }
    
    private fun addDirectoryToTree(parentNode: CheckedTreeNode, directory: File) {
        directory.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name })?.forEach { file ->
            if (!file.name.startsWith(".")) { // Skip hidden files like .git
                val node = CheckedTreeNode(file)
                parentNode.add(node)
                
                if (file.isDirectory) {
                    addDirectoryToTree(node, file)
                }
            }
        }
    }
    
    private fun selectAllFiles(selected: Boolean) {
        val root = fileTree.model.root as? CheckedTreeNode ?: return
        setNodeSelection(root, selected)
        fileTree.repaint()
    }
    
    private fun setNodeSelection(node: CheckedTreeNode, selected: Boolean) {
        node.isChecked = selected
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                setNodeSelection(child, selected)
            }
        }
    }
    
    private fun applyFileTypeFilter() {
        val filterText = fileTypeFilterField.text.trim()
        if (filterText.isEmpty()) {
            selectAllFiles(true)
            return
        }
        
        val extensions = filterText.split(",").map { it.trim().lowercase() }
        val root = fileTree.model.root as? CheckedTreeNode ?: return
        
        applyFilterToNode(root, extensions)
        fileTree.repaint()
    }
    
    private fun applyFilterToNode(node: CheckedTreeNode, extensions: List<String>): Boolean {
        val file = node.userObject as? File
        var hasMatchingChildren = false
        
        // First, check all children
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                val childMatches = applyFilterToNode(child, extensions)
                hasMatchingChildren = hasMatchingChildren || childMatches
            }
        }
        
        // For files, check if extension matches
        val fileMatches = if (file != null && file.isFile) {
            val fileExtension = file.extension.lowercase()
            extensions.any { ext ->
                val cleanExt = ext.removePrefix(".")
                fileExtension == cleanExt || ext == ".$fileExtension"
            }
        } else {
            false
        }
        
        // Select node if it's a matching file or has matching children
        val shouldSelect = fileMatches || hasMatchingChildren
        node.isChecked = shouldSelect
        
        return shouldSelect
    }
    
    private fun updateBranchComboBox(branches: List<String>) {
        branchComboBox.removeAllItems()
        branches.forEach { branchComboBox.addItem(it) }
        if (branches.isEmpty()) {
            branchComboBox.addItem("main")
            branchComboBox.addItem("master")
        }
    }
    
    private fun updateRepositoryInfo(repoUrl: String, branchCount: Int, tagCount: Int) {
        val repoName = repoUrl.substringAfterLast("/").removeSuffix(".git")
        repoInfoLabel.text = "Repository: $repoName | Branches: $branchCount | Tags: $tagCount"
        repoInfoLabel.isVisible = true
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
                    cell(repoInfoLabel)
                        .align(AlignX.LEFT)
                }
                row {
                    label("Select files/folders:")
                }
                row {
                    cell(selectAllButton)
                        .align(AlignX.LEFT)
                    cell(deselectAllButton)
                        .align(AlignX.LEFT)
                }
                row {
                    label("File type filter:")
                }
                row {
                    cell(fileTypeFilterField)
                        .resizableColumn()
                        .align(AlignX.FILL)
                    cell(applyFilterButton)
                        .align(AlignX.RIGHT)
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
        val repoUrl = repoUrlField.text.trim()
        
        return when {
            repoUrl.isNotEmpty() && !validateRepositoryUrl() -> ValidationInfo("Invalid repository URL format", repoUrlField)
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
        val virtualFiles = mutableListOf<VirtualFile>()
        
        clonedRepoPath?.let { repoPath ->
            val repoRoot = gitService.getRepositoryRoot(repoPath)
            if (repoRoot != null) {
                val root = fileTree.model.root as? CheckedTreeNode
                if (root != null) {
                    collectCheckedFiles(root, repoPath, repoRoot, virtualFiles)
                }
            }
        }
        
        return virtualFiles.toTypedArray()
    }
    
    private fun collectCheckedFiles(
        node: CheckedTreeNode,
        repoPath: String,
        repoRoot: VirtualFile,
        virtualFiles: MutableList<VirtualFile>
    ) {
        if (node.isChecked) {
            val file = node.userObject as? File
            if (file != null && file != File(repoPath)) { // Don't include the root directory itself
                val relativePath = File(repoPath).toPath().relativize(file.toPath()).toString().replace('\\', '/')
                val virtualFile = repoRoot.findFileByRelativePath(relativePath)
                if (virtualFile != null) {
                    virtualFiles.add(virtualFile)
                }
            }
        }
        
        // Always check children, even if parent is not checked (for partial selections)
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                collectCheckedFiles(child, repoPath, repoRoot, virtualFiles)
            }
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
    
    override fun doCancelAction() {
        // Cleanup cloned repository if user cancels
        clonedRepoPath?.let { gitService.cleanupRepository(it) }
        super.doCancelAction()
    }
}
