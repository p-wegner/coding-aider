package de.andrena.codingaider.actions.ide

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.ClipboardEditService
import java.awt.datatransfer.DataFlavor

class AiderClipboardEditFormatAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val clipboard = CopyPasteManager.getInstance()

        if (clipboard.areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
            val clipboardText = clipboard.getContents(DataFlavor.stringFlavor) as? String
            if (clipboardText != null && clipboardText.isNotEmpty()) {
                val clipboardEditService = project.service<ClipboardEditService>()
                val changesApplied = clipboardEditService.processText(clipboardText)
                
                if (changesApplied > 0) {
                    showNotification(
                        project, 
                        "Applied $changesApplied edit format changes", 
                        NotificationType.INFORMATION
                    )
                } else {
                    showNotification(
                        project, 
                        "No supported edit format detected in clipboard", 
                        NotificationType.WARNING
                    )
                }
            } else {
                showNotification(project, "Clipboard does not contain text", NotificationType.WARNING)
            }
        } else {
            showNotification(project, "Clipboard does not contain text", NotificationType.WARNING)
        }
    }

    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Aider Clipboard Image")
            .createNotification(content, type)
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
