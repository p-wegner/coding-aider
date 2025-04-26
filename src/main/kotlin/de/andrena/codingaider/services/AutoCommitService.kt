package de.andrena.codingaider.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.settings.AiderSettings
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepositoryManager
import java.io.File

/**
 * Service for handling automatic commits after successful plugin-based edits
 */
@Service(Service.Level.PROJECT)
class AutoCommitService(private val project: Project) {
    private val logger = Logger.getInstance(AutoCommitService::class.java)
    private val settings = AiderSettings.getInstance()
    private val commitMessageExtractor = project.service<CommitMessageExtractorService>()

    fun tryAutoCommit(llmResponse: String, modifiedFiles: List<String>): Boolean {
        // Check if auto-commit is enabled and there are modified files
        if (!isAutoCommitEnabled() || modifiedFiles.isEmpty()) {
            logger.info("Auto-commit skipped: enabled=${isAutoCommitEnabled()}, modifiedFiles=${modifiedFiles.size}")
            return false
        }

        val commitMessage = commitMessageExtractor.extractCommitMessage(llmResponse)
        if (commitMessage.isNullOrBlank()) {
            logger.info("Auto-commit skipped: No commit message found in LLM response")
            showNotification("Auto-commit skipped: No commit message found in LLM response", NotificationType.WARNING)
            return false
        }

        // Store the commit message
        lastCommitMessage = commitMessage
        
        // Perform the commit
        return try {
            performGitCommit(modifiedFiles, commitMessage)
            showNotification("Successfully committed changes: $commitMessage", NotificationType.INFORMATION)
            true
        } catch (e: Exception) {
            logger.error("Failed to auto-commit changes: ${e.message}", e)
            showNotification("Failed to auto-commit changes: ${e.message}", NotificationType.ERROR)
            false
        }
    }

    /**
     * Checks if auto-commit is enabled based on settings
     */
    private fun isAutoCommitEnabled(): Boolean {
        // Check if the auto-commit after edits setting is enabled
        if (!settings.autoCommitAfterEdits) {
            return false
        }

        // Check if plugin-based edits is enabled (required for auto-commit)
        if (!settings.pluginBasedEdits) {
            logger.info("Auto-commit skipped: Plugin-based edits is disabled")
            return false
        }

        // Check if prompt augmentation and commit message block are enabled (required for auto-commit)
        if (!settings.promptAugmentation || !settings.includeCommitMessageBlock) {
            logger.info("Auto-commit skipped: Prompt augmentation or commit message block is disabled")
            return false
        }

        // Also check the global auto-commit setting
        val autoCommitSetting = settings.autoCommits

        return when (autoCommitSetting) {
            AiderSettings.AutoCommitSetting.ON -> true
            AiderSettings.AutoCommitSetting.OFF -> false
            AiderSettings.AutoCommitSetting.DEFAULT -> true // Default is now true if all other conditions are met
        }
    }

    /**
     * Performs a Git commit with the specified files and message
     */
    private fun performGitCommit(filePaths: List<String>, commitMessage: String) {
        val repositoryManager = GitRepositoryManager.getInstance(project)
        val git = Git.getInstance()

        // Convert file paths to VirtualFiles
        // TODO 26.04.2025 pwegner: file paths will be relative to project root, make sure this is properly handled
        val virtualFiles = filePaths.mapNotNull { path ->
            LocalFileSystem.getInstance().findFileByIoFile(File(path))
        }

        if (virtualFiles.isEmpty()) {
            throw IllegalStateException("No valid files to commit")
        }

        // Get the repository for the first file
        val repository = repositoryManager.getRepositoryForFile(virtualFiles.first())
            ?: throw IllegalStateException("No Git repository found for the modified files")

        // Check if dirty commits are allowed
        val allowDirtyCommits = when (settings.dirtyCommits) {
            AiderSettings.DirtyCommitSetting.ON -> true
            AiderSettings.DirtyCommitSetting.OFF -> false
            AiderSettings.DirtyCommitSetting.DEFAULT -> false // Default to not allowing dirty commits
        }

        // Check if there are uncommitted changes in the repository
        if (!allowDirtyCommits) {
            val status = git.status(project, repository.root)
            if (status.hasUncommittedChanges()) {
                throw IllegalStateException("Repository has uncommitted changes. Enable 'Allow Dirty Commits' in settings to commit anyway.")
            }
        }

        // Stage the files
        val handler = GitLineHandler(project, repository.root, GitCommand.ADD)
        virtualFiles.forEach { handler.addParameters(it.path) }
        git.runCommand(handler)

        // Commit the changes
        val commitHandler = GitLineHandler(project, repository.root, GitCommand.COMMIT)
        commitHandler.addParameters("-m", commitMessage)
        git.runCommand(commitHandler)
    }

    /**
     * Shows a notification in the IDE
     */
    private fun showNotification(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(project)
    }
    
    // Store the last commit message for reference
    private var lastCommitMessage: String? = null
    
    /**
     * Returns the last commit message that was used
     */
    fun getLastCommitMessage(): String? {
        return lastCommitMessage
    }
}

private fun Git.status(project: Project, root: VirtualFile): GitStatus {
    val handler = git4idea.commands.GitLineHandler(project, root, git4idea.commands.GitCommand.STATUS)
    handler.addParameters("--porcelain")
    val result = this.runCommand(handler)
    return GitStatus(result.output.isNotEmpty())
}

class GitStatus(private val hasChanges: Boolean) {
    fun hasUncommittedChanges(): Boolean {
        return hasChanges
    }
}
