package de.andrena.codingaider.actions.ide

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.services.AiderHistoryService

class ShowLastCommandResultAction : AnAction() {
    companion object {
        private var activeDialog: MarkdownDialog? = null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showLastCommandFor(project)
    }

    fun showLastCommandFor(project: Project) {
        // If dialog exists and is still valid, bring it to front
        activeDialog?.let { dialog ->
            if (dialog.isDisplayable) {
                dialog.toFront()
                return
            } else {
                activeDialog = null
            }
        }

        // Create new dialog if none exists
        val historyHandler = project.service<AiderHistoryService>()
        val lastCommandResult = historyHandler.getLastChatHistory()

        val dialog = MarkdownDialog.create(project, "Last Aider Command Result", lastCommandResult)
        activeDialog = dialog
        dialog.isVisible = true
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
