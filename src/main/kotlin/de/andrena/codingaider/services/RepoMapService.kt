package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.FileTraversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Service(Service.Level.PROJECT)
class RepoMapService(private val project: Project) {
    private val logger = Logger.getInstance(RepoMapService::class.java)

    suspend fun generateRepoMap(
        includePatterns: List<String> = emptyList(),
        excludePatterns: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        try {
            logger.info("Generating repository map for project: ${project.name}")
            
            val projectPath = project.basePath ?: throw IllegalStateException("Project path is null")
            val projectFiles = getProjectFiles(projectPath, includePatterns, excludePatterns)
            val repoStructure = buildRepoStructure(projectFiles)
            
            """
            Repository Map for ${project.name}
            =====================================
            
            Project Structure:
            $repoStructure
            
            Total Files: ${projectFiles.size}
            Project Path: $projectPath
            """.trimIndent()
            
        } catch (e: Exception) {
            logger.error("Failed to generate repository map", e)
            "Error generating repository map: ${e.message}"
        }
    }

    private fun getProjectFiles(
        projectPath: String,
        includePatterns: List<String>,
        excludePatterns: List<String>
    ): List<String> {
        val projectDir = File(projectPath)
        
        return projectDir.walkTopDown()
            .filter { it.isFile }
            .filter { !shouldExcludeFile(it, excludePatterns) }
            .filter { shouldIncludeFile(it, includePatterns) }
            .map { FileTraversal.normalizedFilePath(it.relativeTo(projectDir).path) }
            .sorted()
            .toList()
    }

    private fun shouldExcludeFile(file: File, excludePatterns: List<String>): Boolean {
        val name = file.name
        val path = FileTraversal.normalizedFilePath(file.path)
        
        // Check custom exclude patterns
        if (excludePatterns.any { pattern -> matchesPattern(path, pattern) }) {
            return true
        }
        
        // Common exclusions
        return name.startsWith(".") ||
                path.contains("/.git/") ||
                path.contains("/node_modules/") ||
                path.contains("/build/") ||
                path.contains("/target/") ||
                path.contains("/.idea/") ||
                path.contains("/out/") ||
                name.endsWith(".class") ||
                name.endsWith(".jar") ||
                name.endsWith(".war") ||
                name.endsWith(".log")
    }

    private fun shouldIncludeFile(file: File, includePatterns: List<String>): Boolean {
        if (includePatterns.isEmpty()) {
            return true
        }
        
        val path = FileTraversal.normalizedFilePath(file.path)
        return includePatterns.any { pattern -> matchesPattern(path, pattern) }
    }

    private fun matchesPattern(path: String, pattern: String): Boolean {
        // Simple glob pattern matching
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        
        return path.matches(Regex(regex))
    }

    private fun buildRepoStructure(files: List<String>): String {
        val structure = StringBuilder()
        val directories = mutableSetOf<String>()
        
        // Collect all directory paths
        files.forEach { file ->
            val parts = file.split("/")
            for (i in 1 until parts.size) {
                val dir = parts.take(i).joinToString("/")
                directories.add(dir)
            }
        }
        
        // Build tree structure
        val allPaths = (directories + files).sorted()
        
        allPaths.forEach { path ->
            val depth = path.count { it == '/' }
            val indent = "  ".repeat(depth)
            val name = path.substringAfterLast("/").ifEmpty { path }
            
            if (files.contains(path)) {
                structure.appendLine("$indent- $name")
            } else {
                structure.appendLine("$indent+ $name/")
            }
        }
        
        return structure.toString()
    }

    companion object {
        fun getInstance(project: Project): RepoMapService =
            project.getService(RepoMapService::class.java)
    }
}
