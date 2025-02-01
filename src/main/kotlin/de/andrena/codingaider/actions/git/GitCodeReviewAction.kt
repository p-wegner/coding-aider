package de.andrena.codingaider.actions.git

import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.GitDiffUtils
import git4idea.GitUtil

class GitCodeReviewAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (!isGitAvailable(project)) {
            showNotification(
                project,
                "This action requires a Git repository",
                NotificationType.ERROR
            )
            return
        }
        
        try {
            val dialog = GitCodeReviewDialog(project)
            if (!dialog.showAndGet()) return

            val (fromRef, toRef) = dialog.getSelectedRefs()
            val prompt = dialog.getPrompt()

            var diffResult: GitDiffUtils.DiffResult? = null
            val success = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    try {
                        diffResult = GitDiffUtils.getChangedFiles(project, fromRef, toRef)
                    } catch (ex: VcsException) {
                        showNotification(
                            project,
                            "Failed to get changed files: ${ex.message}",
                            NotificationType.ERROR
                        )
                        return@runProcessWithProgressSynchronously
                    }
                    true
                },
                "Getting Changed Files...",
                true,
                project
            )
            
            if (!success) return

            val settings = AiderSettings.getInstance()
            // Save diff content to file
            val diffFile = File("${project.basePath}/.coding-aider-plans/git_code_review_diff.md")
            diffFile.parentFile.mkdirs()
            diffFile.writeText("""[Git Code Review Diff]

This file contains the git diff content for code review analysis.

```diff
${diffResult?.diffContent ?: "No diff content available"}
```""")

            // Create command data including the diff file
            val allFiles = (diffResult?.files ?: emptyList()) + 
                FileData(diffFile.absolutePath, true)
            
            val commandData = CommandData(
                message = """Review the code changes between Git refs '$fromRef' and '$toRef'.
                    |
                    |Pay special attention to:
                    |$prompt
                    |
                    |Please analyze and provide:
                    |1. A concise summary of the changes
                    |2. Potential issues, bugs, or security concerns
                    |3. Specific suggestions for improvements
                    |4. Code quality assessment (patterns, practices, maintainability)
                    |5. Performance considerations
                    |
                    |The git diff content is available in the git_code_review_diff.md file."""
                    .trimMargin(),
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = diffResult?.files ?: emptyList(),
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                options = CommandOptions(
                    commitHashToCompareWith = fromRef,
                    summarizedOutput = true
                ),
                sidecarMode = settings.useSidecarMode
            )

            IDEBasedExecutor(project, commandData).execute()

        } catch (ex: Exception) {
            showNotification(
                project,
                "An error occurred during code review: ${ex.message}",NotificationType.ERROR

            )
        }
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
            .getNotificationGroup("Aider Clipboard Image")
            .createNotification(content, type)
            .notify(project)
    }

}
