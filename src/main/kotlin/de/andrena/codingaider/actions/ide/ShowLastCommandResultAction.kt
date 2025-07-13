package de.andrena.codingaider.actions.ide

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.AiderHistoryService
import de.andrena.codingaider.services.AiderOutputService
import javax.swing.SwingUtilities.invokeLater

class ShowLastCommandResultAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showLastCommandFor(project)
    }

    fun showLastCommandFor(project: Project) {
        // Use invokeLater to ensure we're on the EDT
        invokeLater {
            // Create new tab in tool window - do this on a background thread to avoid UI freezes
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                val historyHandler = project.service<AiderHistoryService>()
                val lastCommandResult = historyHandler.getLastChatHistory()
                
                // Switch back to EDT for UI creation
                invokeLater {
                    val outputService = project.service<AiderOutputService>()
                    val output = outputService.createOutput(
                        "Last Aider Command Result",
                        lastCommandResult,
                        null,
                        "Last Command Result",
                        null
                    )
                    outputService.focus(output)
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
