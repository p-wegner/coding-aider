package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.model.ContextFileHandler
import java.io.File

class OpenContextYamlFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        if (!file.name.endsWith(".context.yaml")) {
            return
        }

        val contextFile = File(file.path)
        val projectBasePath = project.basePath ?: return
        
        val files = ContextFileHandler.readContextFile(contextFile, projectBasePath)
        openFiles(files.map { it.filePath }, project)
    }

    private fun openFiles(filePaths: List<String>, project: Project) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        
        filePaths.forEach { path ->
            val file = LocalFileSystem.getInstance().findFileByPath(path)
            file?.let { fileEditorManager.openFile(it, true) }
        }
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file?.name?.endsWith(".context.yaml") == true
    }
}
