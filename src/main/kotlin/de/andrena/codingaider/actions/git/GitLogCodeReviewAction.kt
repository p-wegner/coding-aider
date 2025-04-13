package de.andrena.codingaider.actions.git

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.vcs.log.VcsCommitMetadata
import git4idea.GitUtil

/**
 * Action that can be triggered from the Git log window to review changes between selected commits.
 */
class GitLogCodeReviewAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val commitSelection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION)
        
        // Enable the action only when exactly two commits are selected
        e.presentation.isEnabled = project != null && commitSelection != null && commitSelection.size() == 2
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitSelection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION) ?: return

        if (commitSelection.size() != 2) {
            showNotification(
                project,
                "Please select exactly two commits to compare",
                NotificationType.ERROR
            )
            return
        }

        if (!isGitAvailable(project)) {
            showNotification(
                project,
                "This action requires a Git repository",
                NotificationType.ERROR
            )
            return
        }

        // Get commit hashes from selection
        val commits = commitSelection.cachedMetadata
        if (commits.size < 2) return
        
        // Sort commits chronologically (older first)
        val sortedCommits = commits.sortedBy { it.commitTime }
        val baseCommit = sortedCommits[0].id.asString()
        val targetCommit = sortedCommits[1].id.asString()

        // Launch the code review with the selected commits
        GitCodeReviewAction.performReview(project, baseCommit, targetCommit)
    }

    private fun isGitAvailable(project: Project): Boolean {
        return try {
            GitUtil.getRepositoryManager(project).repositories.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(project)
    }
}
