package de.andrena.codingaider.actions.aider

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.services.AiderEditFormat
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.regex.Pattern

class AiderClipboardEditFormatAction : AnAction() {
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

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val clipboard = CopyPasteManager.getInstance()

        if (clipboard.areDataFlavorsAvailable(DataFlavor.stringFlavor)) {
            val clipboardText = clipboard.getContents(DataFlavor.stringFlavor) as? String
            if (clipboardText != null && clipboardText.isNotEmpty()) {
                processClipboardText(project, clipboardText)
            } else {
                showNotification(project, "Clipboard does not contain text", NotificationType.WARNING)
            }
        } else {
            showNotification(project, "Clipboard does not contain text", NotificationType.WARNING)
        }
    }

    private fun processClipboardText(project: Project, text: String) {
        // Try to detect and apply each format
        when {
            tryApplyDiffFormat(project, text) -> 
                showNotification(project, "Applied diff format changes", NotificationType.INFORMATION)
            
            tryApplyDiffFencedFormat(project, text) -> 
                showNotification(project, "Applied diff-fenced format changes", NotificationType.INFORMATION)
            
            tryApplyWholeFormat(project, text) -> 
                showNotification(project, "Applied whole file replacement", NotificationType.INFORMATION)
            
            tryApplyUdiffFormat(project, text) -> 
                showNotification(project, "Applied udiff format changes", NotificationType.INFORMATION)
            
            else -> 
                showNotification(project, "No supported edit format detected in clipboard", NotificationType.WARNING)
        }
    }

    private fun tryApplyDiffFormat(project: Project, text: String): Boolean {
        val matcher = DIFF_PATTERN.matcher(text)
        var applied = false
        
        while (matcher.find()) {
            val filePath = matcher.group(1).trim()
            val searchText = matcher.group(2)
            val replaceText = matcher.group(3)
            
            if (applyChange(project, filePath, searchText, replaceText)) {
                applied = true
            }
        }
        
        return applied
    }

    private fun tryApplyDiffFencedFormat(project: Project, text: String): Boolean {
        val matcher = DIFF_FENCED_PATTERN.matcher(text)
        var applied = false
        
        while (matcher.find()) {
            val filePath = matcher.group(1).trim()
            val searchText = matcher.group(2)
            val replaceText = matcher.group(3)
            
            if (applyChange(project, filePath, searchText, replaceText)) {
                applied = true
            }
        }
        
        return applied
    }

    private fun tryApplyWholeFormat(project: Project, text: String): Boolean {
        val matcher = WHOLE_PATTERN.matcher(text)
        var applied = false
        
        while (matcher.find()) {
            val filePath = matcher.group(1).trim()
            val newContent = matcher.group(2)
            
            if (replaceWholeFile(project, filePath, newContent)) {
                applied = true
            }
        }
        
        return applied
    }

    private fun tryApplyUdiffFormat(project: Project, text: String): Boolean {
        val matcher = UDIFF_PATTERN.matcher(text)
        var applied = false
        
        while (matcher.find()) {
            val filePath = matcher.group(1).trim()
            val diffContent = matcher.group(2)
            
            if (applyUdiffChange(project, filePath, diffContent)) {
                applied = true
            }
        }
        
        return applied
    }

    private fun applyChange(project: Project, filePath: String, searchText: String, replaceText: String): Boolean {
        val file = findOrCreateFile(project, filePath) ?: return false
        
        try {
            val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            val fileContent = document.text
            if (!fileContent.contains(searchText)) {
                showNotification(
                    project,
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
                project,
                "Error applying changes to $filePath: ${e.message}",
                NotificationType.ERROR
            )
            return false
        }
    }

    private fun replaceWholeFile(project: Project, filePath: String, newContent: String): Boolean {
        val file = findOrCreateFile(project, filePath) ?: return false
        
        try {
            val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(newContent)
            }
            
            return true
        } catch (e: Exception) {
            showNotification(
                project,
                "Error replacing file $filePath: ${e.message}",
                NotificationType.ERROR
            )
            return false
        }
    }

    private fun applyUdiffChange(project: Project, filePath: String, diffContent: String): Boolean {
        val file = findOrCreateFile(project, filePath) ?: return false
        
        try {
            val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)
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
                project,
                "Error applying udiff to $filePath: ${e.message}",
                NotificationType.ERROR
            )
            return false
        }
    }

    private fun findOrCreateFile(project: Project, filePath: String): VirtualFile? {
        val normalizedPath = filePath.replace('\\', '/')
        val projectBasePath = project.basePath
        
        if (projectBasePath == null) {
            showNotification(project, "Project base path not found", NotificationType.ERROR)
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
                    project,
                    "Failed to create file $filePath: ${e.message}",
                    NotificationType.ERROR
                )
                return null
            }
        }
        
        // Refresh VFS to see the new file
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile == null) {
            showNotification(project, "Failed to find or create file: $filePath", NotificationType.ERROR)
            return null
        }
        
        return virtualFile
    }

    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Aider Clipboard Image")
            .createNotification(content, type)
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
