package de.andrena.codingaider.actions.git

import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.services.AiderIgnoreService
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.GitDiffUtils
import git4idea.GitUtil
import java.io.File

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
            // Show dialog to select commits
            val dialog = GitCodeReviewDialog(project)
            if (!dialog.showAndGet()) return

            val (fromRef, toRef) = dialog.getSelectedRefs()
            val prompt = dialog.getPrompt()

            // Perform the review with the selected commits
            performReview(project, fromRef, toRef, prompt)
        } catch (ex: Exception) {
            showNotification(
                project,
                "An error occurred during code review: ${ex.message}",
                NotificationType.ERROR
            )
        }
    }

    companion object {
        /**
         * Performs a code review between two commits.
         * This method can be called directly from other actions with pre-selected commits.
         */
        fun performReview(
            project: Project,
            fromRef: String,
            toRef: String,
            prompt: String? = null
        ) {
            try {
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
                diffFile.writeText(
                    """[Git Code Review Diff]

This file contains the git diff content for code review analysis.

```diff
${diffResult?.diffContent ?: "No diff content available"}
```"""
                )

                // Use default prompt if none provided
                val reviewPrompt = prompt ?: """
                    |1. Code quality and best practices
                    |2. Potential bugs or issues
                    |3. Performance implications
                    |4. Security considerations
                    |5. Design patterns and architecture
                    |6. Test coverage
                    |7. Documentation needs
                """.trimMargin()


                // Start with the changed files from the diff
                val changedVirtualFiles = diffResult?.files?.mapNotNull { fileData ->
                    LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                }?.toTypedArray() ?: emptyArray()

                val allFiles = project.getService(FileDataCollectionService::class.java)
                    .collectAllFiles(changedVirtualFiles, true)
                    .toMutableList()

                // Add the diff file with read-only flag (only if not ignored)
                val aiderIgnoreService = project.service<AiderIgnoreService>()
                if (!aiderIgnoreService.isIgnored(diffFile.absolutePath)) {
                    allFiles.add(FileData(diffFile.absolutePath, true))
                }

                val commandData = CommandData(
                    message = """Review the code changes between Git refs '$fromRef' and '$toRef'.
                        |
                        |Pay special attention to:
                        |$reviewPrompt
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
                    files = allFiles,
                    lintCmd = settings.lintCmd,
                    deactivateRepoMap = settings.deactivateRepoMap,
                    editFormat = settings.editFormat,
                    projectPath = project.basePath ?: "",
                    options = CommandOptions(
                        commitHashToCompareWith = fromRef,
                        promptAugmentation = settings.promptAugmentation
                    ),
                    sidecarMode = settings.useSidecarMode
                )

                IDEBasedExecutor(project, commandData).execute()
            } catch (ex: Exception) {
                showNotification(
                    project,
                    "An error occurred during code review: ${ex.message}",
                    NotificationType.ERROR
                )
            }
        }

        fun showNotification(project: Project, content: String, type: NotificationType) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Coding Aider Notifications")
                .createNotification(content, type)
                .notify(project)
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
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(project)
    }
}
