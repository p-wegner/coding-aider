package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.utils.FileTraversal
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class AiderIgnoreService(private val project: Project) {
    private val ignoreFile = File(project.basePath ?: "", ".aiderignore")
    private var patterns: List<PathMatcher> = emptyList()
    private var rawPatterns: List<String> = emptyList()

    init {
        loadIgnorePatterns()
    }

    fun loadIgnorePatterns() {
        if (ignoreFile.exists()) {
            rawPatterns = ignoreFile.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
            patterns = rawPatterns.map { pattern ->
                val normalizedPattern = pattern.trim().replace('\\', '/')
                val projectRoot = project.basePath?.replace('\\', '/') ?: ""
                
                // For absolute patterns (starting with /), anchor to project root
                // For relative patterns, match anywhere under project root
                val globPattern = when {
                    normalizedPattern.startsWith("/") -> "glob:$projectRoot$normalizedPattern"
                    normalizedPattern.endsWith("/") -> "glob:{$projectRoot/$normalizedPattern**,$projectRoot/**/$normalizedPattern**}"
                    else -> "glob:{$projectRoot/$normalizedPattern,$projectRoot/**/$normalizedPattern}"
                }
                FileSystems.getDefault().getPathMatcher(globPattern)
            }
        } else {
            patterns = emptyList()
            rawPatterns = emptyList()
        }
    }

    fun isIgnored(filePath: String): Boolean {
        if (patterns.isEmpty()) return false
        
        // Normalize both the file path and project root
        val normalizedPath = FileTraversal.normalizedFilePath(filePath)
        val projectRoot = project.basePath?.replace('\\', '/') ?: ""
        
        // Create absolute path for matching
        val absolutePath = Paths.get(normalizedPath)
        
        // Create relative path for matching against project-relative patterns
        val relativePath = if (normalizedPath.startsWith(projectRoot)) {
            Paths.get(normalizedPath.substring(projectRoot.length + 1))
        } else {
            Paths.get(normalizedPath)
        }
        
        return patterns.any { matcher ->
            matcher.matches(absolutePath) || matcher.matches(relativePath)
        }
    }

    fun addPatternToIgnoreFile(pattern: String) {
        if (!ignoreFile.exists()) {
            ignoreFile.createNewFile()
        }
        
        val currentPatterns = if (ignoreFile.exists()) {
            ignoreFile.readLines().toMutableList()
        } else {
            mutableListOf()
        }
        
        if (!currentPatterns.contains(pattern)) {
            currentPatterns.add(pattern)
            ignoreFile.writeText(currentPatterns.joinToString("\n"))
            refreshIgnoreFile()
            loadIgnorePatterns()
        }
    }


    fun createIgnoreFileIfNeeded(): VirtualFile {
        if (!ignoreFile.exists()) {
            ignoreFile.createNewFile()
            // Add some default patterns
            val defaultPatterns = listOf(
                "# Ignore build directories",
                "build/",
                "out/",
                "target/",
                "",
                "# Ignore IDE files",
                ".idea/",
                "*.iml",
                "",
                "# Ignore common generated files",
                "*.class",
                "*.jar",
                "*.war",
                "node_modules/",
                "dist/",
                ""
            )
            ignoreFile.writeText(defaultPatterns.joinToString("\n"))
            refreshIgnoreFile()
            loadIgnorePatterns()
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ignoreFile)!!
    }

    private fun refreshIgnoreFile() {
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ignoreFile)?.refresh(false, false)
    }

}
