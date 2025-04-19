package de.andrena.codingaider.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.regex.Pattern

@Service(Service.Level.PROJECT)
class ClipboardEditService(private val project: Project) {
    companion object {
        // Patterns for different edit formats
        private val DIFF_PATTERN = Pattern.compile(
            """(.+?)\n```(?:\w*)\n<<<<<<< SEARCH\n([\s\S]*?)=======\n([\s\S]*?)>>>>>>> REPLACE\n```""",
            Pattern.MULTILINE
        )
        
        private val DIFF_FENCED_PATTERN = Pattern.compile(
            """```\n(.+?)\n<<<<<<< SEARCH\n([\s\S]*?)=======\n([\s\S]*?)>>>>>>> REPLACE\n```""",
            Pattern.MULTILINE
        )
        
        private val WHOLE_PATTERN = Pattern.compile(
            """(.+?)\n```(?:\w*)\n([\s\S]*?)\n```""",
            Pattern.MULTILINE
        )
        
        private val UDIFF_PATTERN = Pattern.compile(
            """```diff\n--- (.+?)\n\+\+\+ \1\n@@ .* @@\n([\s\S]*?)\n```""",
            Pattern.MULTILINE
        )
    }

    /**
     * Process text that may contain multiple edit blocks in various formats
     * @return The number of successfully applied changes
     */
    fun processText(text: String): Int {
        var appliedChanges = 0
        
        appliedChanges += applyAllMatches(text, DIFF_PATTERN) { filePath, searchText, replaceText ->
            applyChange(filePath, searchText, replaceText)
        }
        
        appliedChanges += applyAllMatches(text, DIFF_FENCED_PATTERN) { filePath, searchText, replaceText ->
            applyChange(filePath, searchText, replaceText)
        }
        
        appliedChanges += applyAllMatches(text, WHOLE_PATTERN) { filePath, newContent, _ ->
            replaceWholeFile(filePath, newContent)
        }
        
        appliedChanges += applyAllMatches(text, UDIFF_PATTERN) { filePath, diffContent, _ ->
            applyUdiffChange(filePath, diffContent)
        }
        
        return appliedChanges
    }
    
    private fun applyAllMatches(
        text: String, 
        pattern: Pattern, 
        applyFn: (String, String, String) -> Boolean
    ): Int {
        val matcher = pattern.matcher(text)
        var count = 0
        
        while (matcher.find()) {
            val filePath = matcher.group(1).trim()
            val content1 = matcher.group(2)
            val content2 = if (matcher.groupCount() >= 3) matcher.group(3) else ""
            
            if (applyFn(filePath, content1, content2)) {
                count++
            }
        }
        
        return count
    }

    fun applyChange(filePath: String, searchText: String, replaceText: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            val fileContent = document.text
            if (!fileContent.contains(searchText)) {
                showNotification(
                    "Search text not found in file: $filePath",
                    NotificationType.ERROR
                )
                return false
            }
            
            WriteCommandAction.runWriteCommandAction(project) {
                val startOffset = fileContent.indexOf(searchText)
                document.replaceString(startOffset, startOffset + searchText.length, replaceText)
            }
            
            return true
        } catch (e: Exception) {
            showNotification(
                "Error applying changes to $filePath: ${e.message}",
                NotificationType.ERROR
            )
            return false
        }
    }

    fun replaceWholeFile(filePath: String, newContent: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(newContent)
            }
            
            return true
        } catch (e: Exception) {
            showNotification(
                "Error replacing file $filePath: ${e.message}",
                NotificationType.ERROR
            )
            return false
        }
    }

    fun applyUdiffChange(filePath: String, diffContent: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            val fileContent = document.text
            val lines = fileContent.lines().toMutableList()
            
            // Process diff lines
            var currentLine = 0
            val diffLines = diffContent.lines()
            
            for (diffLine in diffLines) {
                when {
                    diffLine.startsWith("+") && !diffLine.startsWith("+++") -> {
                        // Add line
                        val lineContent = diffLine.substring(1)
                        lines.add(currentLine, lineContent)
                        currentLine++
                    }
                    diffLine.startsWith("-") && !diffLine.startsWith("---") -> {
                        // Remove line
                        if (currentLine < lines.size) {
                            lines.removeAt(currentLine)
                        }
                    }
                    !diffLine.startsWith("@") -> {
                        // Context line, move to next
                        currentLine++
                    }
                }
            }
            
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(lines.joinToString("\n"))
            }
            
            return true
        } catch (e: Exception) {
            showNotification(
                "Error applying udiff to $filePath: ${e.message}",
                NotificationType.ERROR
            )
            return false
        }
    }

    private fun findOrCreateFile(filePath: String): VirtualFile? {
        val normalizedPath = filePath.replace('\\', '/')
        val projectBasePath = project.basePath
        
        if (projectBasePath == null) {
            showNotification("Project base path not found", NotificationType.ERROR)
            return null
        }
        
        val fullPath = if (normalizedPath.startsWith("/")) {
            normalizedPath
        } else {
            "$projectBasePath/$normalizedPath"
        }
        
        val file = File(fullPath)
        
        // Ensure parent directories exist
        file.parentFile?.mkdirs()
        
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                showNotification(
                    "Failed to create file $filePath: ${e.message}",
                    NotificationType.ERROR
                )
                return null
            }
        }
        
        // Refresh VFS to see the new file
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile == null) {
            showNotification("Failed to find or create file: $filePath", NotificationType.ERROR)
            return null
        }
        
        return virtualFile
    }

    private fun showNotification(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(project)
    }
}
