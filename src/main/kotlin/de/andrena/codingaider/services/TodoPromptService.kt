package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import java.io.File
import java.nio.file.Paths

/**
 * Service that collects TODO comments from a list of files and augments a prompt
 * with these TODOs referencing their source files.
 */
@Service(Service.Level.PROJECT)
class TodoPromptService(private val project: Project) {

    fun buildPrompt(basePrompt: String, files: List<FileData>): String {
        val todosPerFile = collectTodos(files)
        if (todosPerFile.isEmpty()) return basePrompt

        val sb = StringBuilder(basePrompt.trimEnd())
        if (sb.isNotEmpty()) sb.appendLine().appendLine()
        sb.appendLine("Fix the following TODOs:")
        todosPerFile.forEach { (filePath, todos) ->
            val relativePath = project.basePath?.let { base ->
                Paths.get(base).relativize(Paths.get(filePath)).toString()
            } ?: filePath
            sb.appendLine("- In $relativePath:")
            todos.forEach { sb.appendLine(it.trim()) }
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    fun collectTodos(files: List<FileData>): Map<String, List<String>> {
        val todoMap = linkedMapOf<String, List<String>>()
        files.forEach { fileData ->
            val file = File(fileData.filePath)
            if (file.exists() && file.isFile) {
                val todos = file.readLines().filter { it.contains("TODO", ignoreCase = true) }
                if (todos.isNotEmpty()) {
                    todoMap[fileData.filePath] = todos
                }
            }
        }
        return todoMap
    }
}
