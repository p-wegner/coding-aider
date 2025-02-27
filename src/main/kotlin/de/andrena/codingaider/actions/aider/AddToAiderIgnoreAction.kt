package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.services.AiderIgnoreService
import java.io.File

class AddToAiderIgnoreAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        val aiderIgnoreService = project.service<AiderIgnoreService>()
        val ignoreFile = aiderIgnoreService.createIgnoreFileIfNeeded()

        for (file in files) {
            val relativePath = getRelativePath(project, file)
            val pattern = if (file.isDirectory) "$relativePath/" else relativePath
            
            aiderIgnoreService.addPatternToIgnoreFile(pattern)
        }

        Messages.showInfoMessage(
            project,
            "Added ${files.size} pattern(s) to .aiderignore file",
            "AiderIgnore Updated"
        )
    }

    private fun getRelativePath(project: Project, file: VirtualFile): String {
        val projectPath = project.basePath ?: return file.path
        val filePath = file.path
        
        return if (filePath.startsWith(projectPath)) {
            filePath.substring(projectPath.length + 1).replace('\\', '/')
        } else {
            filePath.replace('\\', '/')
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
