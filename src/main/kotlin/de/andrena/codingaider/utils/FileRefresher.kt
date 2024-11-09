package de.andrena.codingaider.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import de.andrena.codingaider.outputview.MarkdownDialog
import java.awt.EventQueue.invokeLater

object FileRefresher {
    fun refreshFiles(files: Array<VirtualFile>, markdownDialog: MarkdownDialog? = null) {
        invokeLater {
            ApplicationManager.getApplication().invokeLater {
                WriteAction.runAndWait<Throwable> {
                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
                    RefreshQueue.getInstance().refresh(true, true, null, *files)
                }
                markdownDialog?.isVisible = true
            }
        }
    }

    fun refreshPath(aiderPlansFolder: String) {
       // TODO: Refresh all files in the folder
        TODO("Not yet implemented")
    }
}
