package de.andrena.codingaider.features.webcrawl

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeListener
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.features.documentation.dialogs.DocumentationGenerationDialog
import de.andrena.codingaider.services.GitRepoCloneService
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultTreeModel
import kotlin.collections.isEmpty
import kotlin.collections.isNotEmpty

class WebCrawlAndGitDialog(private val project: Project) : DialogWrapper(project) {
    private val urlField = JBTextField().apply {
        emptyText.text = "Enter URL to crawl"
    }
    private val logger = Logger.getInstance(WebCrawlAndGitDialog::class.java)
    
    private var selectedTab = 0

    // Git Repository fields
    private val gitService = project.service<GitRepoCloneService>()
    private val repoUrlField = JBTextField().apply {
        emptyText.text = "Enter Git repository URL (https://github.com/user/repo.git)"
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

    private val cloneButton = JButton("Clone Repository").apply {
        addActionListener { cloneRepository() }
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
        addCheckboxTreeListener(object : CheckboxTreeListener {
            override fun nodeStateChanged(node: CheckedTreeNode) {
                // Node state changed - could be used for other purposes
            }
        })
    }

    private val fileTypeFilterField = JBTextField().apply {
        emptyText.text = "Filter by file extensions (e.g., .kt,.java,.md)"
    }

    private val applyFilterButton = JButton("Apply Filter").apply {
        addActionListener { applyFileTypeFilter() }
    }

    private var clonedRepoPath: String? = null
    private var isUpdatingComboBox = false

    init {
        title = "Web Crawl & Git Repository Documentation"
        init()
    }


    override fun createCenterPanel(): JComponent {
        val tabbedPane = JBTabbedPane()

        // Web Crawl Tab
        val webCrawlPanel = panel {
            row {
                label("Enter URL to crawl:")
            }
            row {
                cell(urlField)
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
            row {
                text("This will crawl the web page and process it for documentation.")
            }
        }

        tabbedPane.addTab("Web Crawl", webCrawlPanel)

        // Git Repository Tab with actual functionality
        val gitPanel = panel {
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
                    cell(cloneButton)
                        .align(AlignX.RIGHT)
                }
                row {
                    cell(authCheckBox)
                        .align(AlignX.LEFT)
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
                    cell(JBScrollPane(fileTree).apply {
                        preferredSize = Dimension(700, 400)
                        minimumSize = Dimension(500, 300)
                        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                        viewport.preferredSize = Dimension(680, 380)
                    })
                        .resizableColumn()
                        .align(AlignY.FILL)
                        .align(AlignX.FILL)
                }.resizableRow()
            }
        }

        tabbedPane.addTab("Git Repository", gitPanel)
        
        // Track selected tab changes
        tabbedPane.addChangeListener { 
            selectedTab = tabbedPane.selectedIndex
        }

        val mainPanel = panel {
            row {
                cell(tabbedPane)
                    .resizableColumn()
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
            }.resizableRow()
        }

        mainPanel.preferredSize = Dimension(900, 700)
        mainPanel.minimumSize = Dimension(700, 500)
        return mainPanel
    }

    override fun doValidate(): ValidationInfo? {
        return when (selectedTab) {
            0 -> { // Web Crawl
                val url = urlField.text.trim()
                if (url.isEmpty()) {
                    ValidationInfo("Please enter a URL to crawl", urlField)
                } else {
                    null
                }
            }

            1 -> { // Git Repository
                val repoUrl = repoUrlField.text.trim()
                when {
                    repoUrl.isEmpty() -> ValidationInfo("Please enter a repository URL", repoUrlField)
                    !validateRepositoryUrl() -> ValidationInfo("Invalid repository URL format", repoUrlField)
                    clonedRepoPath == null -> ValidationInfo("Please clone the repository first")
                    getSelectedFiles().isEmpty() -> ValidationInfo("Please select at least one file or folder from the repository")
                    else -> null
                }
            }

            else -> null
        }
    }

    override fun doOKAction() {
        when (selectedTab) {
            0 -> { // Web Crawl
                val url = urlField.text.trim()
                if (url.isNotEmpty()) {
                    super.doOKAction()
                    project.service<WebCrawlService>().performWebCrawl(project, url)
                }
            }

            1 -> { // Git Repository
                val selectedFiles = getSelectedFiles()
                if (selectedFiles.isNotEmpty() && clonedRepoPath != null) {
                    // Create git documentation folder structure with absolute path
                    val repoUrl = repoUrlField.text.trim()
                    val repoName = repoUrl.substringAfterLast("/").removeSuffix(".git")
                    val projectRoot = project.basePath ?: "."
                    val absoluteGitDocsPath = "$projectRoot/$AIDER_DOCS_FOLDER/git-repos/$repoName"
                    File(absoluteGitDocsPath).mkdirs()
                    
                    // Use absolute path for suggested filename so aider saves to correct location
                    val suggestedFilename = "$absoluteGitDocsPath/${repoName}-documentation.md"
                    
                    // Close this dialog and open the standard documentation generation dialog
                    super.doOKAction()
                    
                    // Open DocumentationGenerationDialog with pre-selected files and suggested filename
                    val documentationDialog = DocumentationGenerationDialog(project, selectedFiles, suggestedFilename)
                    documentationDialog.show()
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

        val selectedItem = branchTagComboBox.selectedItem as? BranchTagItem
        val selectedBranch = selectedItem?.name
        val credentials = if (authCheckBox.isSelected) {
            val username = usernameField.text.trim()
            val password = String(passwordField.password)
            if (username.isEmpty() || password.isEmpty()) {
                Messages.showErrorDialog(
                    "Please enter both username and password/token for private repository",
                    "Authentication Required"
                )
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
            false // shallowClone = false to get all branches and tags
        ).thenAccept { result ->
            SwingUtilities.invokeLater {
                cloneButton.isEnabled = true
                cloneButton.text = "Clone Repository"

                if (result.success && result.localPath != null) {
                    logger.info("Cloned repository: $repoUrl into ${result.localPath}")
                    clonedRepoPath = result.localPath
                    updateFileTree(result.localPath)
                    updateBranchTagComboBox(result.branches, result.tags)
                    updateRepositoryInfo(repoUrl, result.branches.size, result.tags.size)

                    // Update UI state to enable OK button
//                        SwingUtilities.invokeLater {
//                            repaint()
//                            doValidate()
//                        }

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
        isUpdatingComboBox = true
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
        isUpdatingComboBox = false
    }

    private fun updateRepositoryInfo(repoUrl: String, branchCount: Int, tagCount: Int) {
        val repoName = repoUrl.substringAfterLast("/").removeSuffix(".git")
        repoInfoLabel.text = "Repository: $repoName | Branches: $branchCount | Tags: $tagCount"
        repoInfoLabel.isVisible = true
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

    private fun switchBranchOrTag() {
        // Avoid triggering during programmatic updates
        if (isUpdatingComboBox || clonedRepoPath == null) {
            return
        }

        val selectedItem = branchTagComboBox.selectedItem as? BranchTagItem ?: return
        val repoDir = File(clonedRepoPath!!)

        branchTagComboBox.isEnabled = false
        
        // Perform checkout in background
        Thread {
            val success = if (selectedItem.isTag) {
                gitService.checkoutTag(repoDir, selectedItem.name)
            } else {
                gitService.switchToBranch(repoDir, selectedItem.name)
            }

            SwingUtilities.invokeLater {
                branchTagComboBox.isEnabled = true
                
                if (success) {
                    // Update file tree with new branch/tag content
                    updateFileTree(clonedRepoPath!!)
                    logger.info("Successfully switched to ${if (selectedItem.isTag) "tag" else "branch"}: ${selectedItem.name}")
                } else {
                    Messages.showErrorDialog(
                        "Failed to switch to ${if (selectedItem.isTag) "tag" else "branch"}: ${selectedItem.name}",
                        "Checkout Error"
                    )
                }
            }
        }.start()
    }

    override fun doCancelAction() {
        // Cleanup cloned repository if user cancels
        clonedRepoPath?.let { gitService.cleanupRepository(it) }
        super.doCancelAction()
    }
}
