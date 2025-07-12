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
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.layout.selected
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
    
    // Constructor for pre-selected files
    constructor(project: Project, preSelectedFiles: Array<VirtualFile>, repoPath: String) : this(project) {
        this.selectedFiles = preSelectedFiles
        this.clonedRepoPath = repoPath
        // Initialize UI with pre-selected data
        SwingUtilities.invokeLater {
            updateFileTreeFromVirtualFiles(preSelectedFiles, repoPath)
            updateRepositoryInfoFromPath(repoPath)
        }
    }
    
    fun setPreSelectedFiles(files: Array<VirtualFile>, repoPath: String) {
        this.selectedFiles = files
        this.clonedRepoPath = repoPath
        SwingUtilities.invokeLater {
            updateFileTreeFromVirtualFiles(files, repoPath)
            updateRepositoryInfoFromPath(repoPath)
            // Update document types to ensure they're loaded
            updateDocumentTypes()
            repaint()
        }
    }
    
    private val settings = AiderProjectSettings.getInstance(project)
    private val gitService = project.service<GitRepoCloneService>()
    private val settingsButton = createSettingsButton()
    
    // Git Repository fields
    private val repoUrlField = JBTextField().apply {
        emptyText.text = "Enter Git repository URL (https://github.com/user/repo.git)"
        addActionListener { validateRepositoryUrl() }
    }
    
    private val branchTagComboBox = ComboBox<BranchTagItem>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (component is JLabel && value is BranchTagItem) {
                    text = value.displayName
                    icon = if (value.isTag) AllIcons.Nodes.Tag else AllIcons.Vcs.Branch
                }
                return component
            }
        }
        addActionListener { switchBranchOrTag() }
    }
    
    data class BranchTagItem(
        val name: String,
        val isTag: Boolean,
        val displayName: String = if (isTag) "tag: $name" else "branch: $name"
    )
    
    private val usernameField = JBTextField().apply {
        emptyText.text = "Username (for private repos)"
        isVisible = false
    }
    
    private val passwordField = JBPasswordField().apply {
        emptyText.text = "Password/Token (for private repos)"
        isVisible = false
    }
    
    private val authCheckBox = JCheckBox("Private repository (requires authentication)").apply {
        addActionListener { toggleAuthFields() }
    }
    
    private val shallowCloneCheckBox = JCheckBox("Shallow clone (faster, recommended for large repos)", true)
    
    private val checkRepoButton = JButton("Check Repository").apply {
        addActionListener { checkRepository() }
    }
    
    private val cloneButton = JButton("Clone Repository").apply {
        addActionListener { cloneRepository() }
        isEnabled = false
    }
    
    private val repoInfoLabel = JLabel("").apply {
        isVisible = false
    }
    
    private val repoSizeLabel = JLabel("").apply {
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
    
    private fun checkRepository() {
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
        
        val credentials = if (authCheckBox.isSelected) {
            val username = usernameField.text.trim()
            val password = String(passwordField.password)
            if (username.isEmpty() || password.isEmpty()) {
                Messages.showErrorDialog("Please enter both username and password/token for private repository", "Authentication Required")
                return
            }
            GitRepoCloneService.AuthCredentials(username, password)
        } else {
            null
        }
        
        checkRepoButton.isEnabled = false
        checkRepoButton.text = "Checking..."
        
        gitService.getRepositoryInfoAsync(repoUrl, credentials).thenAccept { repoInfo ->
            SwingUtilities.invokeLater {
                checkRepoButton.isEnabled = true
                checkRepoButton.text = "Check Repository"
                
                if (repoInfo.isAccessible) {
                    val repoName = repoUrl.substringAfterLast("/").removeSuffix(".git")
                    repoInfoLabel.text = "Repository: $repoName | Default branch: ${repoInfo.defaultBranch ?: "unknown"}"
                    repoInfoLabel.isVisible = true
                    
                    if (repoInfo.estimatedSizeMB != null) {
                        val sizeText = when {
                            repoInfo.estimatedSizeMB < 1.0 -> "< 1 MB"
                            repoInfo.estimatedSizeMB < 100.0 -> String.format("~%.1f MB", repoInfo.estimatedSizeMB)
                            repoInfo.estimatedSizeMB < 1000.0 -> String.format("~%.0f MB", repoInfo.estimatedSizeMB)
                            else -> String.format("~%.1f GB", repoInfo.estimatedSizeMB / 1024.0)
                        }
                        repoSizeLabel.text = "Estimated size: $sizeText"
                        repoSizeLabel.isVisible = true
                        
                        if (repoInfo.estimatedSizeMB > 500.0) {
                            repoSizeLabel.text += " (Large repository - consider shallow clone)"
                        }
                    }
                    
                    // Set default branch if available
                    repoInfo.defaultBranch?.let { defaultBranch ->
                        val defaultItem = BranchTagItem(defaultBranch, false)
                        branchTagComboBox.addItem(defaultItem)
                        branchTagComboBox.selectedItem = defaultItem
                    }
                    
                    cloneButton.isEnabled = true
                    Messages.showInfoMessage("Repository is accessible and ready to clone!", "Repository Check")
                } else if (repoInfo.requiresAuth && !authCheckBox.isSelected) {
                    Messages.showErrorDialog(
                        "This repository requires authentication. Please check 'Private repository' and enter your credentials.",
                        "Authentication Required"
                    )
                    authCheckBox.isSelected = true
                    toggleAuthFields()
                } else {
                    Messages.showErrorDialog(
                        "Failed to access repository: ${repoInfo.error ?: "Unknown error"}",
                        "Repository Check Error"
                    )
                }
            }
        }
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
        
        val selectedItem = branchTagComboBox.selectedItem as? BranchTagItem
        val selectedBranch = selectedItem?.name
        val credentials = if (authCheckBox.isSelected) {
            val username = usernameField.text.trim()
            val password = String(passwordField.password)
            if (username.isEmpty() || password.isEmpty()) {
                Messages.showErrorDialog("Please enter both username and password/token for private repository", "Authentication Required")
                return
            }
            GitRepoCloneService.AuthCredentials(username, password)
        } else {
            null
        }
        
        cloneButton.isEnabled = false
        cloneButton.text = "Cloning..."
        
        gitService.cloneRepositoryAsync(
            repoUrl, 
            selectedBranch, 
            null, // targetCommit
            credentials,
            shallowCloneCheckBox.isSelected
        ).thenAccept { result ->
            SwingUtilities.invokeLater {
                cloneButton.isEnabled = true
                cloneButton.text = "Clone Repository"
                
                if (result.success && result.localPath != null) {
                    clonedRepoPath = result.localPath
                    updateFileTree(result.localPath)
                    updateBranchTagComboBox(result.branches, result.tags)
                    updateRepositoryInfo(repoUrl, result.branches.size, result.tags.size)
                    Messages.showInfoMessage("Repository cloned successfully!", "Success")
                } else if (result.requiresAuth && !authCheckBox.isSelected) {
                    Messages.showErrorDialog(
                        "This repository requires authentication. Please check 'Private repository' and enter your credentials.",
                        "Authentication Required"
                    )
                    authCheckBox.isSelected = true
                    toggleAuthFields()
                } else {
                    Messages.showErrorDialog(
                        "Failed to clone repository: ${result.error ?: "Unknown error"}",
                        "Clone Error"
                    )
                }
            }
        }
    }
    
    private fun toggleAuthFields() {
        val isVisible = authCheckBox.isSelected
        usernameField.isVisible = isVisible
        passwordField.isVisible = isVisible
        repaint()
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
    
    private fun updateBranchTagComboBox(branches: List<String>, tags: List<String>) {
        // Remove all existing action listeners
        branchTagComboBox.actionListeners.forEach { branchTagComboBox.removeActionListener(it) }
        branchTagComboBox.removeAllItems()
        
        // Add branches first
        branches.forEach { branch ->
            branchTagComboBox.addItem(BranchTagItem(branch, false))
        }
        
        // Add tags
        tags.forEach { tag ->
            branchTagComboBox.addItem(BranchTagItem(tag, true))
        }
        
        // Add default branches if none exist
        if (branches.isEmpty() && tags.isEmpty()) {
            branchTagComboBox.addItem(BranchTagItem("main", false))
            branchTagComboBox.addItem(BranchTagItem("master", false))
        }
        
        branchTagComboBox.addActionListener { switchBranchOrTag() }
    }
    
    private fun switchBranchOrTag() {
        val selectedItem = branchTagComboBox.selectedItem as? BranchTagItem
        val repoPath = clonedRepoPath
        
        if (selectedItem != null && repoPath != null) {
            val repoDir = File(repoPath)
            if (repoDir.exists()) {
                val success = if (selectedItem.isTag) {
                    gitService.checkoutTag(repoDir, selectedItem.name)
                } else {
                    gitService.switchToBranch(repoDir, selectedItem.name)
                }
                
                if (success) {
                    // Refresh file tree after branch/tag switch
                    updateFileTree(repoPath)
                    val type = if (selectedItem.isTag) "tag" else "branch"
                    Messages.showInfoMessage("Switched to $type: ${selectedItem.name}", "Checkout Success")
                } else {
                    val type = if (selectedItem.isTag) "tag" else "branch"
                    Messages.showErrorDialog(
                        "Failed to switch to $type: ${selectedItem.name}",
                        "Checkout Error"
                    )
                }
            }
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
                    cell(branchTagComboBox)
                        .align(AlignX.LEFT)
                    cell(checkRepoButton)
                        .align(AlignX.CENTER)
                    cell(cloneButton)
                        .align(AlignX.RIGHT)
                }
                row {
                    cell(authCheckBox)
                        .align(AlignX.LEFT)
                    cell(shallowCloneCheckBox)
                        .align(AlignX.RIGHT)
                }
                row {
                    label("Username:")
                    cell(usernameField)
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .visibleIf(authCheckBox.selected)
                }
                row {
                    label("Password/Token:")
                    cell(passwordField)
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .visibleIf(authCheckBox.selected)
                }
                row {
                    cell(repoInfoLabel)
                        .align(AlignX.LEFT)
                }
                row {
                    cell(repoSizeLabel)
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
            // Only validate repo URL if it's not empty (for cases where repo is already cloned)
            repoUrl.isNotEmpty() && !validateRepositoryUrl() -> ValidationInfo("Invalid repository URL format", repoUrlField)
            // Allow validation to pass if we have a cloned repo path (pre-selected scenario)
            clonedRepoPath == null && repoUrl.isEmpty() -> ValidationInfo("Please clone a repository first or enter a repository URL")
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
    
    private fun updateFileTreeFromVirtualFiles(files: Array<VirtualFile>, repoPath: String) {
        if (files.isNotEmpty()) {
            val root = CheckedTreeNode(File(repoPath).name)
            root.userObject = File(repoPath)
            
            val repoDir = File(repoPath)
            if (repoDir.exists()) {
                addDirectoryToTree(root, repoDir)
                // Pre-select the provided files
                preselectFiles(root, files, repoPath)
            }
            
            fileTree.model = DefaultTreeModel(root)
            fileTree.expandRow(0)
            
            // Expand some levels to show the structure
            for (i in 0 until minOf(3, fileTree.rowCount)) {
                fileTree.expandRow(i)
            }
        }
    }
    
    private fun preselectFiles(root: CheckedTreeNode, files: Array<VirtualFile>, repoPath: String) {
        val filePaths = files.map { 
            // Convert VirtualFile path to relative path from repo root
            val repoFile = File(repoPath)
            val virtualPath = File(it.path)
            try {
                repoFile.toPath().relativize(virtualPath.toPath()).toString().replace('\\', '/')
            } catch (e: Exception) {
                // Fallback to just the file name if relativization fails
                it.name
            }
        }.toSet()
        preselectNodesRecursively(root, filePaths, repoPath)
    }
    
    private fun preselectNodesRecursively(node: CheckedTreeNode, filePaths: Set<String>, repoPath: String) {
        val file = node.userObject as? File
        if (file != null) {
            val relativePath = try {
                File(repoPath).toPath().relativize(file.toPath()).toString().replace('\\', '/')
            } catch (e: Exception) {
                file.name
            }
            
            // Check if this file/directory should be selected
            val shouldSelect = filePaths.any { targetPath ->
                relativePath == targetPath || 
                relativePath.endsWith(targetPath) || 
                targetPath.endsWith(relativePath) ||
                file.name == targetPath
            }
            
            if (shouldSelect) {
                node.isChecked = true
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                preselectNodesRecursively(child, filePaths, repoPath)
            }
        }
    }
    
    private fun updateRepositoryInfoFromPath(repoPath: String) {
        val repoName = File(repoPath).name
        repoInfoLabel.text = "Repository: $repoName | Cloned locally | Ready for documentation"
        repoInfoLabel.isVisible = true
        
        // Also populate some default filename if empty
        if (filenameField.text.isBlank()) {
            filenameField.text = "${repoName}_documentation.md"
        }
    }

    override fun doCancelAction() {
        // Cleanup cloned repository if user cancels
        clonedRepoPath?.let { gitService.cleanupRepository(it) }
        super.doCancelAction()
    }
}
