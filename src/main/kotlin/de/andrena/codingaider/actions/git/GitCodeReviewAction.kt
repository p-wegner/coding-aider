package de.andrena.codingaider.actions.git

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.showNotification
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.dialog.GitCodeReviewDialog
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings

class GitCodeReviewAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        if (!isGitAvailable(project)) {
            showNotification(
                project,
                "Git Not Available",
                "This action requires a Git repository",
                true
            )
            return
        }
        
        try {
            val dialog = GitCodeReviewDialog(project)
            if (!dialog.showAndGet()) return

            val (fromRef, toRef) = dialog.getSelectedRefs()
            val prompt = dialog.getPrompt()

            val files = try {
                GitDiffUtils.getChangedFiles(project, fromRef, toRef)
            } catch (ex: VcsException) {
                showNotification(
                    project,
                    "Git Diff Error", 
                    "Failed to get changed files: ${ex.message}",
                    true
                )
                return
            }

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
                "Git Review Error",
                "An error occurred during code review: ${ex.message}",
                true
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
}
