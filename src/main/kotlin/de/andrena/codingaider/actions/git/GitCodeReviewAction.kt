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
        performGitCodeReview(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun performGitCodeReview(project: Project) {
        val dialog = GitCodeReviewDialog(project)
        if (!dialog.showAndGet()) return

        val settings = AiderSettings.getInstance()
        val commandData = CommandData(
            message = dialog.getPrompt(),
            useYesFlag = settings.useYesFlag,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs,
            files = dialog.getSelectedFiles(),
            lintCmd = settings.lintCmd,
            deactivateRepoMap = settings.deactivateRepoMap,
            editFormat = settings.editFormat,
            projectPath = project.basePath ?: "",
            options = CommandOptions(
                commitHashToCompareWith = dialog.getBaseCommit()
            ),
            sidecarMode = settings.useSidecarMode
        )

        IDEBasedExecutor(project, commandData).execute()
    }
}
