package de.andrena.aidershortcut.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import de.andrena.aidershortcut.outputview.MarkdownDialog
import java.awt.EventQueue.invokeLater

object FileRefresher {
    fun refreshFiles(project: Project, files: Array<VirtualFile>, markdownDialog: MarkdownDialog? = null) {
        invokeLater {
            ApplicationManager.getApplication().invokeLater {
                WriteAction.runAndWait<Throwable> {
                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
                    RefreshQueue.getInstance().refresh(true, true, null, *files)
                    FileEditorManager.getInstance(project).reloadFiles(*files)
                }
                markdownDialog?.show()
            }
        }
    }
}
package de.andrena.aidershortcut.utils

import com.intellij.openapi.vfs.VirtualFile
import de.andrena.aidershortcut.command.FileData

object FileTraversal {
    fun traverseFileOrDirectory(file: VirtualFile, isReadOnly: Boolean = false): List<FileData> {
        return if (file.isDirectory) {
            traverseDirectory(file, isReadOnly)
        } else {
            listOf(FileData(file.path, isReadOnly))
        }
    }

    private fun traverseDirectory(directory: VirtualFile, isReadOnly: Boolean = false): List<FileData> {
        val allFiles = mutableListOf<FileData>()
        directory.children.forEach { childFile ->
            allFiles.addAll(traverseFileOrDirectory(childFile, isReadOnly))
        }
        return allFiles
    }
}
