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
                val normalizedPattern = pattern.trim()
                    .replace('\\', '/')
                    .let {
                        when {
                            it.startsWith("/") -> "glob:${project.basePath?.replace('\\', '/')}$it"
                            else -> "glob:${project.basePath?.replace('\\', '/')}/**/$it"
                        }
                    }
                FileSystems.getDefault().getPathMatcher(normalizedPattern)
            }
        } else {
            patterns = emptyList()
            rawPatterns = emptyList()
        }
    }

    fun isIgnored(filePath: String): Boolean {
        if (patterns.isEmpty()) return false
        
        val normalizedPath = FileTraversal.normalizedFilePath(filePath)
        val path = Paths.get(normalizedPath)
        return patterns.any { it.matches(path) }
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

    fun getIgnoreFile(): VirtualFile? {
        if (!ignoreFile.exists()) {
            return null
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ignoreFile)
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

    fun getRawPatterns(): List<String> = rawPatterns
}
