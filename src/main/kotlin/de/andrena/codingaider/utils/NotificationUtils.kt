package de.andrena.codingaider.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData

/**
 * Utility functions for showing notifications in the IDE.
 */
object NotificationUtils {
    
    /**
     * Shows a balloon notification to the user.
     * @param message The message to show
     * @param project The project context
     * @param type The notification type
     */
    fun showBalloonNotification(message: String, project: Project?, type: NotificationType = NotificationType.INFORMATION) {
        project ?: return
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(message, type)
            .notify(project)
    }
    
    /**
     * Shows an error notification.
     * @param message The error message
     * @param project The project context
     */
    fun showError(message: String, project: Project?) {
        showBalloonNotification(message, project, NotificationType.ERROR)
    }
    
    /**
     * Shows an information notification.
     * @param message The information message
     * @param project The project context
     */
    fun showInfo(message: String, project: Project?) {
        showBalloonNotification(message, project, NotificationType.INFORMATION)
    }
    
    /**
     * Shows a warning notification.
     * @param message The warning message
     * @param project The project context
     */
    fun showWarning(message: String, project: Project?) {
        showBalloonNotification(message, project, NotificationType.WARNING)
    }
}

/**
 * Legacy compatibility alias for AiderUtils.
 * This provides backward compatibility during the refactoring.
 */
object AiderUtils {
    enum class NotificationType {
        INFO, WARNING, ERROR
    }
    
    fun showBalloonNotification(message: String, project: Project?, type: NotificationType = NotificationType.INFO) {
        val notificationType = when (type) {
            NotificationType.ERROR -> com.intellij.notification.NotificationType.ERROR
            NotificationType.WARNING -> com.intellij.notification.NotificationType.WARNING
            NotificationType.INFO -> com.intellij.notification.NotificationType.INFORMATION
        }
        NotificationUtils.showBalloonNotification(message, project, notificationType)
    }

    fun getSelectedFiles(event: AnActionEvent): List<FileData> {
        // TODO 11.09.2025 pwegner: Implement file selection retrieval
        return listOf<FileData>()
    }
}