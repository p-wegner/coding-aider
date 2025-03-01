package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.Disposable
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.utils.FileTraversal
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class AiderIgnoreService(private val project: Project) : Disposable {
    private val ignoreFile = File(project.basePath ?: "", ".aiderignore")
    private var patterns: List<PathMatcher> = emptyList()
    private var lastModified: Long = 0

    init {
        loadIgnorePatterns()
    }

    override fun dispose() {
        // Nothing to dispose
    }

    private fun convertToGlobPattern(pattern: String, projectRoot: String): String {
        val normalizedPattern = pattern.trim().replace('\\', '/')
        
        // Skip empty patterns and comments
        if (normalizedPattern.isEmpty() || normalizedPattern.startsWith("#")) {
            return ""
        }

        // Remove leading slash if present to make pattern relative to project root
        val relativePattern = if (normalizedPattern.startsWith("/")) {
            normalizedPattern.substring(1)
        } else {
            normalizedPattern
        }

        return when {
            // Handle directory patterns with trailing slash (folder/)
            relativePattern.endsWith("/") -> {
                val dirPattern = relativePattern.dropLast(1)
                "glob:$projectRoot/$dirPattern/**"
            }
            
            // Handle double-star patterns (**/tmp)
            relativePattern.startsWith("**/") -> {
                val restPattern = relativePattern.substring(3)
                if (restPattern.endsWith("/")) {
                    "glob:$projectRoot/**/${restPattern.dropLast(1)}/**"
                } else {
                    "glob:$projectRoot/**/$restPattern"
                }
            }
            
            // Handle file extension patterns (*.spec.ts)
            relativePattern.startsWith("*.") -> {
                "glob:$projectRoot/**/*${relativePattern.substring(1)}"
            }
            
            // Handle all other patterns
            else -> "glob:$projectRoot/$relativePattern"
        }
    }

    private fun loadIgnorePatterns() {
        if (!ignoreFile.exists()) {
            patterns = emptyList()
            return
        }

        val currentLastModified = ignoreFile.lastModified()
        if (currentLastModified == lastModified) {
            return
        }

        lastModified = currentLastModified
        val projectRoot = project.basePath?.replace('\\', '/') ?: ""
        
        patterns = ignoreFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { pattern -> 
                val globPattern = convertToGlobPattern(pattern, projectRoot)
                if (globPattern.isNotEmpty()) {
                    FileSystems.getDefault().getPathMatcher(globPattern)
                } else {
                    null
                }
            }

        // Notify any services that depend on ignore patterns
        project.messageBus.syncPublisher(PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC)
            .onPersistentFilesChanged()
    }

    fun isIgnored(filePath: String): Boolean {
        loadIgnorePatterns() // Check for updates
        if (patterns.isEmpty()) return false
        
        val normalizedPath = FileTraversal.normalizedFilePath(filePath)
        return patterns.any { it.matches(Paths.get(normalizedPath)) }
    }

    fun addPatternToIgnoreFile(pattern: String) {
        if (!ignoreFile.exists()) {
            ignoreFile.createNewFile()
        }
        
        val currentPatterns = ignoreFile.readLines().toMutableList()
        if (!currentPatterns.contains(pattern)) {
            currentPatterns.add(pattern)
            ignoreFile.writeText(currentPatterns.joinToString("\n"))
            loadIgnorePatterns()
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ignoreFile)?.refresh(false, false)
        }
    }

    fun createIgnoreFileIfNeeded(): VirtualFile {
        if (!ignoreFile.exists()) {
            ignoreFile.createNewFile()
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
            loadIgnorePatterns()
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ignoreFile)!!
    }
}
