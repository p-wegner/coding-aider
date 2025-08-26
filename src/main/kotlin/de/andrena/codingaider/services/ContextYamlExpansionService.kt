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
     * Finds all .context.yaml files in the provided existingFiles list and expands them
     */
    fun expandContextYamlFiles(existingFiles: List<FileData>): List<FileData> {
        val projectBasePath = project.basePath ?: return emptyList()
        
        // Only process .context.yaml files that are in the existingFiles list
        val contextYamlFiles = existingFiles
            .filter { it.filePath.endsWith(".context.yaml") }
            .mapNotNull { fileData ->
                val file = File(fileData.filePath)
                if (file.exists()) file else null
            }
        
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
    
}
