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
import de.andrena.codingaider.dialog.GitCodeReviewDialog
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

            val files = mutableListOf<FileData>()
            val success = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    try {
                        files.addAll(GitDiffUtils.getChangedFiles(project, fromRef, toRef))
                    } catch (ex: VcsException) {
                        showNotification(
                            project,
                            "Failed to get changed files: ${ex.message}",
                            NotificationType.ERROR
                        )
                        return@runProcessWithProgressSynchronously false
                    }
                    true
                },
                "Getting Changed Files...",
                true,
                project
            )
            
            if (!success) return

            val settings = AiderSettings.getInstance()
            val commandData = CommandData(
                message = """Review the code changes between Git refs '$fromRef' and '$toRef'.
                    |Focus on: $prompt
                    |
                    |Please provide:
                    |1. A summary of the changes
                    |2. Potential issues or concerns
                    |3. Suggestions for improvements
                    |4. Code quality assessment""".trimMargin(),
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = files,
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
