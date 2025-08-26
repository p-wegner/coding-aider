package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.model.ContextFileHandler
import java.io.File

@Service(Service.Level.PROJECT)
class ContextYamlExpansionService(private val project: Project) {

    /**
     * Finds all .context.yaml files in the project and expands their content
     */
    fun expandContextYamlFiles(existingFiles: List<FileData>): List<FileData> {
        val projectBasePath = project.basePath ?: return emptyList()
        val contextYamlFiles = findContextYamlFiles(projectBasePath)
        
        val expandedFiles = mutableListOf<FileData>()
        
        contextYamlFiles.forEach { contextFile ->
            try {
                val filesFromContext = ContextFileHandler.readContextFile(contextFile, projectBasePath)
                expandedFiles.addAll(filesFromContext)
            } catch (e: Exception) {
                // Log error but continue processing other context files
                println("Error expanding context YAML file ${contextFile.name}: ${e.message}")
            }
        }
        
        // Remove duplicates by normalized file path, preferring existing files over expanded ones
        val existingNormalizedPaths = existingFiles.map { it.normalizedFilePath }.toSet()
        val uniqueExpandedFiles = expandedFiles.filterNot { 
            existingNormalizedPaths.contains(it.normalizedFilePath) 
        }
        
        return uniqueExpandedFiles
    }
    
    /**
     * Recursively finds all .context.yaml files in the project directory
     */
    private fun findContextYamlFiles(projectBasePath: String): List<File> {
        val projectDir = File(projectBasePath)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return emptyList()
        }
        
        val contextFiles = mutableListOf<File>()
        findContextYamlFilesRecursive(projectDir, contextFiles)
        return contextFiles
    }
    
    private fun findContextYamlFilesRecursive(directory: File, contextFiles: MutableList<File>) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isFile && file.name.endsWith(".context.yaml") -> {
                    contextFiles.add(file)
                }
                file.isDirectory && !shouldSkipDirectory(file.name) -> {
                    findContextYamlFilesRecursive(file, contextFiles)
                }
            }
        }
    }
    
    /**
     * Skip common directories that typically don't contain context files
     */
    private fun shouldSkipDirectory(dirName: String): Boolean {
        return dirName.startsWith(".") && dirName != ".aider" ||
                dirName in setOf("node_modules", "target", "build", "out", "dist", ".gradle", ".idea")
    }
}
