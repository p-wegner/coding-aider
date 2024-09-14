package de.andrena.codingaider.actions.ide

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import de.andrena.codingaider.history.AiderHistoryHandler
import de.andrena.codingaider.outputview.MarkdownDialog

class ShowLastCommandResultAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val historyHandler = AiderHistoryHandler(project.basePath ?: "")
        val lastCommandResult = historyHandler.getLastChatHistory()

        val dialog = MarkdownDialog(project, "Last Aider Command Result", lastCommandResult)
        dialog.setImmediateClose()
        dialog.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
