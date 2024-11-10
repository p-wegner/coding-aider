package de.andrena.codingaider.actions.ide

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.services.AiderHistoryService

class ShowLastCommandResultAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val historyHandler = project.service<AiderHistoryService>()
        val lastCommandResult = historyHandler.getLastChatHistory()

        val dialog = MarkdownDialog.create(project, "Last Aider Command Result", lastCommandResult)
        dialog.isVisible = true
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
