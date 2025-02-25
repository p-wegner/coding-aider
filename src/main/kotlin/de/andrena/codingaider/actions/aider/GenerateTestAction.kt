package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import de.andrena.codingaider.features.testgeneration.dialogs.TestGenerationDialog
import de.andrena.codingaider.settings.AiderProjectSettings

class GenerateTestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        
        try {
            val settings = AiderProjectSettings.getInstance(project)
            val enabledTestTypes = settings.getTestTypes().filter { it.isEnabled }
            
            if (enabledTestTypes.isEmpty()) {
                Messages.showErrorDialog(
                    project,
                    "No test types are configured. Please configure test types in Project Settings.",
                    "No Test Types Available"
                )
                return
            }
            
            TestGenerationDialog(project, files).show()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                project,
                "Failed to generate tests: ${e.message}",
                "Test Generation Error"
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
