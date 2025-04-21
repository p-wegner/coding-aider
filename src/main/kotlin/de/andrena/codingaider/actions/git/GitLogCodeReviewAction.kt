package de.andrena.codingaider.actions.git

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.vcs.log.VcsLogCommitSelection
import com.intellij.vcs.log.VcsLogDataKeys

class GitLogCodeReviewAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val commitSelection: VcsLogCommitSelection? = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION)
        
        // Enable the action only when exactly two commits are selected
        e.presentation.isEnabled = project != null && commitSelection != null && commitSelection.commits.size == 2
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitSelection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION) ?: return

        if (commitSelection.commits.size != 2) {
            GitCodeReviewAction.showNotification(
                project,
                "Please select exactly two commits to compare",
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

        // Show dialog with prefilled commits
        val dialog = GitCodeReviewDialog(project, baseCommit, targetCommit)
        if (dialog.showAndGet()) {
            GitCodeReviewAction.performReview(
                project, 
                dialog.getSelectedRefs().first,
                dialog.getSelectedRefs().second,
                dialog.getPrompt()
            )
        }
    }
}
