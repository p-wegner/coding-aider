package de.andrena.codingaider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.inputdialog.PersistentFileManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.FileTraversal

class PersistentFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val persistentFileManager = PersistentFileManager(project.basePath ?: "")
            val persistentFiles = persistentFileManager.getPersistentFiles()

            val allFiles = FileTraversal.traverseFilesOrDirectories(files, true)

            val allFilesContained = allFiles.all { file ->
                persistentFiles.any { it.filePath == file.filePath }
            }

            val message = if (allFilesContained) {
                allFiles.forEach { file ->
                    persistentFileManager.removeFile(file.filePath)
                }
                "${allFiles.size} file(s) removed from persistent files."
            } else {
                val filesToAdd = allFiles.filter { file ->
                    !persistentFiles.any { it.filePath == file.filePath }
                }
                persistentFileManager.addAllFiles(filesToAdd)
                "${filesToAdd.size} file(s) added to persistent files."
            }

            FileRefresher.refreshFiles(project, arrayOf(persistentFileManager.getContextFile()))

            // Show notification
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Coding Aider Notifications")
                .createNotification(message, NotificationType.INFORMATION)
                .notify(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val persistentFileManager = project?.basePath?.let { PersistentFileManager(it) }
        val persistentFiles = persistentFileManager?.getPersistentFiles() ?: emptyList()

        val allFiles = files?.let { FileTraversal.traverseFilesOrDirectories(it) } ?: emptyList()
        val allFilesContained = allFiles.isNotEmpty() && allFiles.all { file ->
            persistentFiles.any { it.filePath == file.filePath }
        }

        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
        e.presentation.text = if (allFilesContained) "Remove from Persistent Files" else "Add to Persistent Files"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
