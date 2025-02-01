package de.andrena.codingaider.actions.workingdirectory

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.utils.FileTraversal

class SetWorkingDirectoryAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!virtualFile.isDirectory) return

        val settings = AiderProjectSettings.getInstance(project)
        settings.workingDirectory = virtualFile.path

        // Show notification
        showNotification(project, virtualFile)

        // Notify via project message bus that working directory changed
        project.messageBus.syncPublisher(WorkingDirectoryTopic.TOPIC)
            .onWorkingDirectoryChanged(virtualFile.path)
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile?.isDirectory == true
    }

    private fun showNotification(project: Project, file: VirtualFile) {
        val normalizedPath = FileTraversal.normalizedFilePath(file.path)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(
                "Working Directory Set",
                "Aider operations will be restricted to: $normalizedPath",
                NotificationType.INFORMATION
            )
            .notify(project)
    }
}

object WorkingDirectoryTopic {
    val TOPIC = com.intellij.util.messages.Topic.create(
        "WorkingDirectoryChanged",
        WorkingDirectoryListener::class.java
    )
}

interface WorkingDirectoryListener {
    fun onWorkingDirectoryChanged(path: String)
}
