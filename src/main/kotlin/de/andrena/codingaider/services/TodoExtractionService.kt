package de.andrena.codingaider.services

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import de.andrena.codingaider.command.FileData
import java.io.File

@Service(Service.Level.PROJECT)
class TodoExtractionService(private val project: Project) {
    
    /**
     * Extracts all TODO comments from a single PSI file
     */
    fun getTodosFromFile(psiFile: PsiFile): List<PsiComment> {
        return ReadAction.compute<List<PsiComment>, RuntimeException> {
            PsiTreeUtil.findChildrenOfType(psiFile, PsiComment::class.java)
                .filter { it.text.contains("TODO", ignoreCase = true) }
        }
    }
    
    /**
     * Checks if a PSI file contains any TODO comments
     */
    fun hasTodos(psiFile: PsiFile): Boolean {
        return getTodosFromFile(psiFile).isNotEmpty()
    }
    
    /**
     * Gets the text of all TODO comments from a PSI file
     */
    fun getTodoText(psiFile: PsiFile): String {
        return ReadAction.compute<String, RuntimeException> {
            val todos = getTodosFromFile(psiFile)
            todos.joinToString("\n") { it.text }
        }
    }
    
    /**
     * Creates a formatted prompt for fixing TODOs in a specific file
     */
    fun createFixTodoPrompt(todoText: String, psiFile: PsiFile): String {
        return "Fix the TODO in ${psiFile.name}:\n$todoText"
    }
    
    /**
     * Collects all TODOs from multiple files and formats them with file attribution
     */
    fun collectTodosFromFiles(files: List<FileData>): String {
        val todoSections = mutableListOf<String>()
        val psiManager = PsiManager.getInstance(project)
        
        files.forEach { fileData ->
            try {
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                if (virtualFile?.exists() == true && !virtualFile.isDirectory) {
                    val psiFile = ReadAction.compute<PsiFile?, RuntimeException> {
                        psiManager.findFile(virtualFile)
                    }
                    
                    if (psiFile != null && hasTodos(psiFile)) {
                        val todoText = getTodoText(psiFile)
                        val relativePath = getRelativePath(fileData.filePath)
                        todoSections.add("TODOs in $relativePath:\n$todoText")
                    }
                }
            } catch (e: Exception) {
                // Log error but continue processing other files
                println("Error processing file ${fileData.filePath}: ${e.message}")
            }
        }
        
        return if (todoSections.isNotEmpty()) {
            "Fix the following TODOs:\n\n" + todoSections.joinToString("\n\n")
        } else {
            ""
        }
    }
    
    /**
     * Gets a relative path for display purposes
     */
    private fun getRelativePath(filePath: String): String {
        return try {
            val projectBasePath = project.basePath
            if (projectBasePath != null) {
                val projectPath = File(projectBasePath).toPath()
                val targetPath = File(filePath).toPath()
                projectPath.relativize(targetPath).toString().replace('\\', '/')
            } else {
                File(filePath).name
            }
        } catch (e: Exception) {
            File(filePath).name
        }
    }
    
    companion object {
        fun getInstance(project: Project): TodoExtractionService {
            return project.getService(TodoExtractionService::class.java)
        }
    }
}
