package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class MyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && files != null && files.isNotEmpty()) {
            val filePaths = files.joinToString("\n") { it.path }
            Messages.showInfoMessage(project, "Selected files:\n$filePaths", "Your Plugin")
        }
    }

    override fun update(e: AnActionEvent) {
        // Enable the action only if multiple files are selected
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && files != null && files.isNotEmpty()
    }
}