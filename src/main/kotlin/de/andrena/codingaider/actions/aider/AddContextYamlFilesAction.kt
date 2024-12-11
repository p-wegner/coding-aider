package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.model.ContextFileHandler
import de.andrena.codingaider.services.PersistentFileService
import java.io.File

class AddContextYamlFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        selectedFiles
            .filter { it.name.endsWith(".context.yaml") }
            .forEach { contextYamlFile -> 
                addFilesFromContextYaml(project, contextYamlFile)
            }
    }

    override fun update(e: AnActionEvent) {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        e.presentation.isEnabledAndVisible = selectedFiles.any { it.name.endsWith(".context.yaml") }
    }

    private fun addFilesFromContextYaml(project: Project, contextYamlFile: VirtualFile) {
        val persistentFileService = project.getService(PersistentFileService::class.java)
        val contextFile = File(contextYamlFile.path)
        
        val filesToAdd = ContextFileHandler.readContextFile(contextFile, project.basePath ?: "")
        persistentFileService.addAllFiles(filesToAdd)
    }
}
