package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.aidershortcut.command.FileData
import de.andrena.aidershortcut.inputdialog.PersistentFileManager
import de.andrena.aidershortcut.utils.FileRefresher
import de.andrena.aidershortcut.utils.FileTraversal

class PersistentFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val persistentFileManager = PersistentFileManager(project.basePath ?: "")
            val persistentFiles = persistentFileManager.getPersistentFiles()

            val allFiles = files.flatMap { file ->
                if (file.isDirectory) FileTraversal.traverseDirectory(file, true)
                else listOf(FileData(file.path, true))
            }

            val allFilesContained = allFiles.all { file ->
                persistentFiles.any { it.filePath == file.filePath }
            }

            if (allFilesContained) {
                allFiles.forEach { file ->
                    persistentFileManager.removeFile(file.filePath)
                }
            } else {
                val filesToAdd = allFiles.filter { file ->
                    !persistentFiles.any { it.filePath == file.filePath }
                }

                persistentFileManager.addAllFiles(filesToAdd)

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
