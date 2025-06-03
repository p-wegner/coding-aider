package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import de.andrena.codingaider.features.customactions.dialogs.CustomActionDialog
import de.andrena.codingaider.settings.AiderProjectSettings

class ExecuteCustomActionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        
        try {
            val settings = AiderProjectSettings.getInstance(project)
            val enabledCustomActions = settings.getCustomActions().filter { it.isEnabled }
            
            if (enabledCustomActions.isEmpty()) {
                Messages.showErrorDialog(
                    project,
                    "No custom actions are configured. Please configure custom actions in Project Settings.",
                    "No Custom Actions Available"
                )
                return
            }
            
            CustomActionDialog(project, files).show()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to execute custom action: ${e.message}",
                "Custom Action Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
