package de.andrena.codingaider.features.documentation.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import de.andrena.codingaider.services.GitRepoCloneService
import java.awt.Component
import java.io.File
import javax.swing.*

class GitRepositoryPanel(private val project: Project) {
    private val gitService = project.service<GitRepoCloneService>()
    
    data class BranchTagItem(
        val name: String,
        val isTag: Boolean,
        val displayName: String = if (isTag) "tag: $name" else "branch: $name"
    )
    
    data class CloneResult(
        val success: Boolean,
        val repoPath: String?,
        val branches: List<String>,
        val tags: List<String>,
        val error: String?
    )
    
    // UI Components
    val repoUrlField = JBTextField().apply {
        emptyText.text = "Enter Git repository URL (https://github.com/user/repo.git)"
        addActionListener { validateRepositoryUrl() }
    }
    
    val branchTagComboBox = ComboBox<BranchTagItem>().apply {
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
    }
    
    private val usernameField = JBTextField().apply {
        emptyText.text = "Username (for private repos)"
        isVisible = false
    }
    
    private val passwordField = JBPasswordField().apply {
        emptyText.text = "Password/Token (for private repos)"
        isVisible = false
    }
    
    val authCheckBox = JCheckBox("Private repository (requires authentication)").apply {
        addActionListener { toggleAuthFields() }
    }
    
    val shallowCloneCheckBox = JCheckBox("Shallow clone (faster, recommended for large repos)", true)
    
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
    
    // Callbacks
    var onCloneSuccess: ((CloneResult) -> Unit)? = null
    var onBranchTagSwitch: ((BranchTagItem) -> Unit)? = null
    
    private var clonedRepoPath: String? = null
    
    fun createPanel() = panel {
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
        }
    }
    
    fun validateRepositoryUrl(): Boolean {
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
    
    private fun toggleAuthFields() {
        val isVisible = authCheckBox.isSelected
        usernameField.isVisible = isVisible
        passwordField.isVisible = isVisible
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

        val credentials = getCredentials() ?: return

        // Clear previous info
        repoInfoLabel.isVisible = false
        repoSizeLabel.isVisible = false

        checkRepoButton.isEnabled = false
        checkRepoButton.text = "Checking..."

        gitService.getRepositoryInfoAsync(repoUrl, credentials).thenAccept { repoInfo ->
            SwingUtilities.invokeLater {
                checkRepoButton.isEnabled = true
                checkRepoButton.text = "Check Repository"

                if (repoInfo.isAccessible) {
                    val repoName = repoUrl.substringAfterLast("/").removeSuffix(".git")
                    repoInfoLabel.text =
                        "✓ Repository: $repoName | Default branch: ${repoInfo.defaultBranch ?: "unknown"}"
                    repoInfoLabel.isVisible = true

                    if (repoInfo.estimatedSizeMB != null) {
                        val sizeText = when {
                            repoInfo.estimatedSizeMB < 1.0 -> "< 1 MB"
                            repoInfo.estimatedSizeMB < 100.0 -> String.format("~%.1f MB", repoInfo.estimatedSizeMB)
                            repoInfo.estimatedSizeMB < 1000.0 -> String.format("~%.0f MB", repoInfo.estimatedSizeMB)
                            else -> String.format("~%.1f GB", repoInfo.estimatedSizeMB / 1024.0)
                        }
                        repoSizeLabel.text = "📊 Estimated size: $sizeText"
                        repoSizeLabel.isVisible = true

                        if (repoInfo.estimatedSizeMB > 500.0) {
                            repoSizeLabel.text += " ⚠️ (Large repository - consider shallow clone)"
                        }
                    }

                    // Clear existing items and set default branch if available
                    branchTagComboBox.removeAllItems()
                    repoInfo.defaultBranch?.let { defaultBranch ->
                        val defaultItem = BranchTagItem(defaultBranch, false)
                        branchTagComboBox.addItem(defaultItem)
                        branchTagComboBox.selectedItem = defaultItem
                    }

                    cloneButton.isEnabled = true

                    // Show success notification
                    val notificationGroup = com.intellij.notification.NotificationGroupManager.getInstance()
                        .getNotificationGroup("Aider Web Crawl")
                    notificationGroup.createNotification(
                        "Repository Check Successful",
                        "Repository is accessible and ready to clone!",
                        com.intellij.notification.NotificationType.INFORMATION
                    ).notify(project)

                } else if (repoInfo.requiresAuth && !authCheckBox.isSelected) {
                    repoInfoLabel.text = "🔒 Authentication required for this repository"
                    repoInfoLabel.isVisible = true
                    authCheckBox.isSelected = true
                    toggleAuthFields()
                    Messages.showWarningDialog(
                        "This repository requires authentication. Please enter your credentials below.",
                        "Authentication Required"
                    )
                } else {
                    repoInfoLabel.text = "❌ Repository check failed"
                    repoInfoLabel.isVisible = true
                    Messages.showErrorDialog(
                        "Failed to access repository: ${repoInfo.error ?: "Unknown error"}",
                        "Repository Check Error"
                    )
                }
            }
        }.exceptionally { throwable ->
            SwingUtilities.invokeLater {
                checkRepoButton.isEnabled = true
                checkRepoButton.text = "Check Repository"
                repoInfoLabel.text = "❌ Repository check failed"
                repoInfoLabel.isVisible = true
                Messages.showErrorDialog(
                    "Unexpected error during repository check: ${throwable.message}",
                    "Repository Check Error"
                )
            }
            null
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
        val credentials = getCredentials() ?: return

        // Disable UI elements during cloning
        setUIEnabled(false)
        cloneButton.text = "Cloning..."

        gitService.cloneRepositoryAsync(
            repoUrl,
            selectedBranch,
            null, // targetCommit
            credentials,
            shallowCloneCheckBox.isSelected
        ).thenAccept { result ->
            SwingUtilities.invokeLater {
                setUIEnabled(true)
                cloneButton.text = "Clone Repository"

                if (result.success && result.localPath != null) {
                    clonedRepoPath = result.localPath
                    updateBranchTagComboBox(result.branches, result.tags)
                    updateRepositoryInfo(repoUrl, result.branches.size, result.tags.size)

                    // Notify success
                    onCloneSuccess?.invoke(
                        CloneResult(
                            success = true,
                            repoPath = result.localPath,
                            branches = result.branches,
                            tags = result.tags,
                            error = null
                        )
                    )

                    // Show success notification
                    val notificationGroup = com.intellij.notification.NotificationGroupManager.getInstance()
                        .getNotificationGroup("Aider Web Crawl")
                    notificationGroup.createNotification(
                        "Repository Cloned Successfully",
                        "Repository cloned successfully! Please select files and configure documentation settings below, then click OK to generate documentation.",
                        com.intellij.notification.NotificationType.INFORMATION
                    ).notify(project)

                } else if (result.requiresAuth && !authCheckBox.isSelected) {
                    Messages.showErrorDialog(
                        "This repository requires authentication. Please check 'Private repository' and enter your credentials.",
                        "Authentication Required"
                    )
                    authCheckBox.isSelected = true
                    toggleAuthFields()
                } else {
                    onCloneSuccess?.invoke(
                        CloneResult(
                            success = false,
                            repoPath = null,
                            branches = emptyList(),
                            tags = emptyList(),
                            error = result.error
                        )
                    )
                    Messages.showErrorDialog(
                        "Failed to clone repository: ${result.error ?: "Unknown error"}",
                        "Clone Error"
                    )
                }
            }
        }.exceptionally { throwable ->
            SwingUtilities.invokeLater {
                setUIEnabled(true)
                cloneButton.text = "Clone Repository"
                onCloneSuccess?.invoke(
                    CloneResult(
                        success = false,
                        repoPath = null,
                        branches = emptyList(),
                        tags = emptyList(),
                        error = throwable.message
                    )
                )
                Messages.showErrorDialog(
                    "Unexpected error during cloning: ${throwable.message}",
                    "Clone Error"
                )
            }
            null
        }
    }
    
    private fun getCredentials(): GitRepoCloneService.AuthCredentials? {
        return if (authCheckBox.isSelected) {
            val username = usernameField.text.trim()
            val password = String(passwordField.password)
            if (username.isEmpty() || password.isEmpty()) {
                Messages.showErrorDialog(
                    "Please enter both username and password/token for private repository",
                    "Authentication Required"
                )
                return null
            }
            GitRepoCloneService.AuthCredentials(username, password)
        } else {
            null
        }
    }
    
    private fun setUIEnabled(enabled: Boolean) {
        repoUrlField.isEnabled = enabled
        branchTagComboBox.isEnabled = enabled
        checkRepoButton.isEnabled = enabled
        cloneButton.isEnabled = enabled
        authCheckBox.isEnabled = enabled
        shallowCloneCheckBox.isEnabled = enabled
        usernameField.isEnabled = enabled
        passwordField.isEnabled = enabled
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

        branchTagComboBox.addActionListener { 
            val selectedItem = branchTagComboBox.selectedItem as? BranchTagItem
            if (selectedItem != null) {
                onBranchTagSwitch?.invoke(selectedItem)
            }
        }
    }
    
    private fun updateRepositoryInfo(repoUrl: String, branchCount: Int, tagCount: Int) {
        val repoName = repoUrl.substringAfterLast("/").removeSuffix(".git")
        repoInfoLabel.text = "Repository: $repoName | Branches: $branchCount | Tags: $tagCount"
        repoInfoLabel.isVisible = true
    }
    
    fun switchBranchOrTag(item: BranchTagItem): Boolean {
        val repoPath = clonedRepoPath ?: return false
        val repoDir = File(repoPath)
        
        if (!repoDir.exists()) return false
        
        val success = if (item.isTag) {
            gitService.checkoutTag(repoDir, item.name)
        } else {
            gitService.switchToBranch(repoDir, item.name)
        }
        
        if (success) {
            val type = if (item.isTag) "tag" else "branch"
            Messages.showInfoMessage("Switched to $type: ${item.name}", "Checkout Success")
        } else {
            val type = if (item.isTag) "tag" else "branch"
            Messages.showErrorDialog(
                "Failed to switch to $type: ${item.name}",
                "Checkout Error"
            )
        }
        
        return success
    }
    
    fun getClonedRepoPath(): String? = clonedRepoPath
    
    fun cleanup() {
        clonedRepoPath?.let { gitService.cleanupRepository(it) }
    }
    
    fun hide() {
        repoUrlField.isVisible = false
        branchTagComboBox.isVisible = false
        checkRepoButton.isVisible = false
        cloneButton.isVisible = false
        authCheckBox.isVisible = false
        shallowCloneCheckBox.isVisible = false
        usernameField.isVisible = false
        passwordField.isVisible = false
        repoInfoLabel.isVisible = false
        repoSizeLabel.isVisible = false
    }
}
