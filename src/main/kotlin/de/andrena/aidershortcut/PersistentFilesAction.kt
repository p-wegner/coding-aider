package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.aidershortcut.command.FileData
import de.andrena.aidershortcut.inputdialog.PersistentFileManager
import de.andrena.aidershortcut.utils.FileRefresher

class PersistentFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val persistentFileManager = PersistentFileManager(project.basePath ?: "")
            val persistentFiles = persistentFileManager.getPersistentFiles()

            val allFilesContained = files.all { file ->
                persistentFiles.any { it.filePath == file.path }
            }

            if (allFilesContained) {
                // Remove files from persistent list
                files.forEach { file ->
                    persistentFileManager.removeFile(file.path)
                }
            } else {
                // Add files to persistent list
                val filesToAdd = files.filter { file ->
                    !persistentFiles.any { it.filePath == file.path }
                }.map { FileData(it.path, true) }

                persistentFileManager.addAllFiles(filesToAdd)
                
                // Refresh the context file after modifying the persistent list
                FileRefresher.refreshFiles(project, arrayOf(persistentFileManager.getContextFile()))
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val persistentFileManager = project?.basePath?.let { PersistentFileManager(it) }
        val persistentFiles = persistentFileManager?.getPersistentFiles() ?: emptyList()

        val allFilesContained = files?.all { file ->
            persistentFiles.any { it.filePath == file.path }
        } ?: false

        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
        e.presentation.text = if (allFilesContained) "Remove from Persistent Files" else "Add to Persistent Files"
    }
}
