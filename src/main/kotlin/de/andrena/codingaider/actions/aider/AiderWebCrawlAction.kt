package de.andrena.codingaider.actions.aider

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.features.documentation.dialogs.GitRepoDocumentationDialog
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER
import de.andrena.codingaider.services.AiderEditFormat
import de.andrena.codingaider.services.GitRepoCloneService
import de.andrena.codingaider.services.MarkdownConversionService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import java.awt.Dimension
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class AiderWebCrawlAction : AnAction() {

    private class WebCrawlAndGitDialog(private val project: Project) : DialogWrapper(project) {
        private val urlField = JBTextField().apply {
            emptyText.text = "Enter URL to crawl"
        }
        
        // Git Repository fields
        private val gitService = project.service<GitRepoCloneService>()
        private val repoUrlField = JBTextField().apply {
            emptyText.text = "Enter Git repository URL (https://github.com/user/repo.git)"
        }
        
        private val branchComboBox = ComboBox<String>().apply {
            addItem("main")
            addItem("master")
            isEditable = true
        }
        
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
        }
        
        private val applyFilterButton = JButton("Apply Filter").apply {
            addActionListener { applyFileTypeFilter() }
        }
        
        private var clonedRepoPath: String? = null

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
                        cell(branchComboBox)
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
            }

            tabbedPane.addTab("Git Repository", gitPanel)

            val mainPanel = panel {
                row {
                    cell(tabbedPane)
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .align(AlignY.FILL)
                }.resizableRow()
            }

            mainPanel.preferredSize = Dimension(800, 600)
            mainPanel.minimumSize = Dimension(600, 400)
            return mainPanel
        }

        override fun doValidate(): ValidationInfo? {
            val centerComponent = createCenterPanel()
            val selectedTab = (centerComponent as? JComponent)?.let { panel ->
                val tabbedPane = findTabbedPane(panel)
                tabbedPane?.selectedIndex
            } ?: 0

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
                        repoUrl.isNotEmpty() && !validateRepositoryUrl() -> ValidationInfo("Invalid repository URL format", repoUrlField)
                        clonedRepoPath == null && repoUrl.isNotEmpty() -> ValidationInfo("Please clone the repository first")
                        clonedRepoPath != null && getSelectedFiles().isEmpty() -> ValidationInfo("Please select at least one file or folder from the repository")
                        else -> null
                    }
                }
                else -> null
            }
        }

        override fun doOKAction() {
            val centerComponent = createCenterPanel()
            val selectedTab = (centerComponent as? JComponent)?.let { panel ->
                val tabbedPane = findTabbedPane(panel)
                tabbedPane?.selectedIndex
            } ?: 0

            when (selectedTab) {
                0 -> { // Web Crawl
                    val url = urlField.text.trim()
                    if (url.isNotEmpty()) {
                        super.doOKAction()
                        performWebCrawl(project, url)
                    }
                }

                1 -> { // Git Repository
                    val selectedFiles = getSelectedFiles()
                    if (selectedFiles.isNotEmpty()) {
                        super.doOKAction()
                        // Open the full GitRepoDocumentationDialog with pre-selected files
                        val gitDialog = GitRepoDocumentationDialog(project)
                        gitDialog.show()
                    }
                }
            }
        }

        private fun findTabbedPane(component: JComponent): JBTabbedPane? {
            if (component is JBTabbedPane) return component
            for (child in component.components) {
                if (child is JComponent) {
                    val found = findTabbedPane(child)
                    if (found != null) return found
                }
            }
            return null
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
                            branchComboBox.selectedItem = defaultBranch
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
            
            val selectedBranch = branchComboBox.selectedItem as? String
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
                        updateBranchComboBox(result.branches)
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
        
        override fun doCancelAction() {
            // Cleanup cloned repository if user cancels
            clonedRepoPath?.let { gitService.cleanupRepository(it) }
            super.doCancelAction()
        }
    }


    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = WebCrawlAndGitDialog(project)
        dialog.show()
    }

    companion object {
        private fun performWebCrawl(project: Project, url: String) {
            val settings = getInstance()
            val projectRoot = project.basePath ?: "."
            val domain = URI(url).host
            val docsPath = "$projectRoot/$AIDER_DOCS_FOLDER/$domain"
            File(docsPath).mkdirs()

            val combinedHash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).let {
                BigInteger(1, it).toString(16).padStart(32, '0')
            }

            val pageName = URI(url).toURL().path.split("/").lastOrNull() ?: "index"
            val fileName = "$pageName-raw-$combinedHash.md"
            val filePath = "$docsPath/$fileName"
            val file = File(filePath)

            if (!file.exists()) {
                crawlAndProcessWebPage(url, file, project)

                val processedFileName = "$pageName-$combinedHash.md"
                val processedFilePath = "$docsPath/$processedFileName"

                val commandData = CommandData(
                    message = """
                    Clean up and simplify the provided file $fileName. Follow these guidelines:
                    1. Remove all navigation elements, headers, footers, and sidebars.
                    2. Delete any advertisements, banners, or promotional content.
                    3. Remove or simplify the table of contents, keeping only if it's essential for understanding the structure.
                    4. Strip out all internal page links, but keep external links that point to important resources.
                    5. Remove any repetitive elements or boilerplate text.
                    6. Simplify complex HTML structures, converting them to clean markdown where possible.
                    7. Keep all information relevant for code documentation and how to use the technology described.
                    8. Organize the content in a logical flow, using appropriate markdown headers.
                    9. If code snippets are present, ensure they are properly formatted in markdown code blocks.
                    10. Remove any content that seems out of context or irrelevant to the main topic.
                    11. Summarize lengthy paragraphs while retaining key information.
                    12. Ensure the final document is concise, well-structured, and focused on the core technical content.
                    Important: Make sure to save the simplified markdown documentation in a separate file named $processedFileName and not in the same file as the initial content.
                } 
                """.trimIndent(),
                    useYesFlag = true,
                    llm = settings.webCrawlLlm,
                    additionalArgs = "",
                    files = listOf(FileData(filePath, false)),
                    lintCmd = "",
                    projectPath = project.basePath ?: "",
                    editFormat = AiderEditFormat.WHOLE.value,
                    aiderMode = AiderMode.NORMAL,
                    options = CommandOptions(autoCommit = false, dirtyCommits = false, promptAugmentation = false),
                )

                if (settings.activateIdeExecutorAfterWebcrawl) {
                    val executor = IDEBasedExecutor(project, commandData) { success ->
                        if (success && File(processedFilePath).exists()) {
                            // Only add the processed markdown file to persistent files
                            refreshAndAddFile(project, processedFilePath)
                            showNotification(
                                project,
                                "Web page crawled and processed. The processed file has been added to persistent files.",
                                NotificationType.INFORMATION
                            )
                        } else {
                            showNotification(
                                project,
                                "Web page crawled but processing failed or file not found.",
                                NotificationType.WARNING
                            )
                        }
                    }
                    executor.execute()
                } else {
                    // If not using IDE executor, just add the raw file
                    refreshAndAddFile(project, filePath)
                    showNotification(
                        project,
                        "Web page crawled. Raw file has been added to persistent files.",
                        NotificationType.INFORMATION
                    )
                }
            } else {
                // Notify the user that the file already exists
                showNotification(project, "The file already exists. No action taken.", NotificationType.INFORMATION)
            }
        }


        private fun crawlAndProcessWebPage(url: String, file: File, project: Project) {
            val webClient = WebClient()
            webClient.options.apply {
                isJavaScriptEnabled = false
                isThrowExceptionOnScriptError = false
                isThrowExceptionOnFailingStatusCode = false
                isCssEnabled = false
            }

            val page: HtmlPage = webClient.getPage(url)
            val htmlContent = page.asXml()

            val markdown = project.getService(MarkdownConversionService::class.java)
                .convertHtmlToMarkdown(htmlContent, url)

            file.writeText(markdown)
        }

        private fun refreshAndAddFile(project: Project, filePath: String) {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(filePath))
            val persistentFileService = project.getService(PersistentFileService::class.java)
            persistentFileService.addFile(FileData(filePath, true))
            if (virtualFile != null) {
                FileRefresher.refreshFiles(arrayOf(virtualFile))
            }
        }

        private fun showNotification(
            project: Project,
            content: String,
            type: NotificationType
        ) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Aider Web Crawl")
                .createNotification(content, type)
                .notify(project)
        }
    }
}

