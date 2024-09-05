package de.andrena.codingaider

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import de.andrena.codingaider.commandhistory.AiderHistoryHandler

class ShowLastCommandResultAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        if (project != null) {
            val historyHandler = AiderHistoryHandler(project.basePath ?: "")
            val lastCommandResult = historyHandler.getLastChatHistory()
            Messages.showInfoMessage(lastCommandResult, "Last Aider Command Result")
        }
    }
}
