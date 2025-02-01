package de.andrena.codingaider.actions.git

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.dialog.GitCodeReviewDialog
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings

class GitCodeReviewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val dialog = GitCodeReviewDialog(project)
            if (!dialog.showAndGet()) return

            val (fromRef, toRef) = dialog.getSelectedRefs()
            val prompt = dialog.getPrompt()

            val files = try {
                GitDiffUtils.getChangedFiles(project, fromRef, toRef)
            } catch (ex: VcsException) {
                NotificationUtils.showError(
                    project,
                    "Git Diff Error",
                    "Failed to get changed files: ${ex.message}"
                )
                return
            }

            if (files.isEmpty()) {
                NotificationUtils.showWarning(
                    project,
                    "No Changes",
                    "No changes found between $fromRef and $toRef"
                )
                return
            }

            val settings = AiderSettings.getInstance()
            val commandData = CommandData(
                message = """Review the code changes between Git refs '$fromRef' and '$toRef'.
                    |Focus on: $prompt""".trimMargin(),
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = files,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                options = CommandOptions(
                    commitHashToCompareWith = fromRef
                ),
                sidecarMode = settings.useSidecarMode
            )

            IDEBasedExecutor(project, commandData).execute()

        } catch (ex: Exception) {
            NotificationUtils.showError(
                project,
                "Git Review Error",
                "An error occurred during code review: ${ex.message}"
            )
        }
    }
}
