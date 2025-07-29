package de.andrena.codingaider.providers.parsers

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.providers.AIProvider
import de.andrena.codingaider.providers.OutputParser
import java.io.File
import java.nio.file.Paths

/**
 * Output parser for Claude Code responses
 * Claude Code might use different output formats than Aider
 */
class ClaudeCodeOutputParser(private val project: Project) : OutputParser {
    override val provider: AIProvider = AIProvider.CLAUDE_CODE
    
    private val logger = Logger.getInstance(ClaudeCodeOutputParser::class.java)
    private val modifiedFiles = mutableSetOf<String>()
    
    companion object {
        // Claude Code might use JSON-based edits or different formats
        // These are placeholder patterns - actual implementation would depend on Claude Code's output format
        
        // Pattern for Claude Code API-style edits (hypothetical)
        private val API_EDIT_REGEX = """```claude-edit\n(.+?)\n```""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // Pattern for Claude Code file operations (hypothetical)
        private val FILE_OPERATION_REGEX = """// FILE: (.+?)\n([\s\S]*?)(?=// FILE:|$)""".toRegex()
        
        // Standard search/replace format that Claude Code might also support
        private val SEARCH_REPLACE_REGEX = """(.+?)\n```(?:\w*)\n<<<<<<< SEARCH\n([\s\S]*?)\n=======\n([\s\S]*?)\n>>>>>>> REPLACE\n```""".toRegex()
    }
    
    override fun parseBlocks(text: String): List<OutputParser.EditBlock> {
        val blocks = mutableListOf<OutputParser.EditBlock>()
        
        // Try different parsing strategies based on Claude Code's output format
        
        // 1. Try API-style edits first (hypothetical Claude Code format)
        parseApiEdits(text, blocks)
        
        // 2. Try file operation format
        parseFileOperations(text, blocks)
        
        // 3. Fall back to standard search/replace format
        parseSearchReplaceBlocks(text, blocks)
        
        return blocks
    }
    
    private fun parseApiEdits(text: String, blocks: MutableList<OutputParser.EditBlock>) {
        // This is hypothetical - Claude Code might use JSON or other structured formats
        API_EDIT_REGEX.findAll(text).forEach { match ->
            try {
                val editContent = match.groupValues[1]
                // Parse the edit content (could be JSON, YAML, or other format)
                // This is a placeholder implementation
                parseEditContent(editContent)?.let { blocks.add(it) }
            } catch (e: Exception) {
                logger.warn("Error parsing Claude Code API edit: ${e.message}", e)
            }
        }
    }
    
    private fun parseFileOperations(text: String, blocks: MutableList<OutputParser.EditBlock>) {
        FILE_OPERATION_REGEX.findAll(text).forEach { match ->
            try {
                val filePath = match.groupValues[1].trim()
                val content = match.groupValues[2].trim()
                
                blocks.add(OutputParser.EditBlock(
                    filePath = filePath,
                    replaceContent = content,
                    editType = OutputParser.EditType.WHOLE_FILE
                ))
            } catch (e: Exception) {
                logger.warn("Error parsing Claude Code file operation: ${e.message}", e)
            }
        }
    }
    
    private fun parseSearchReplaceBlocks(text: String, blocks: MutableList<OutputParser.EditBlock>) {
        SEARCH_REPLACE_REGEX.findAll(text).forEach { match ->
            try {
                val filePath = match.groupValues[1].trim()
                val searchContent = match.groupValues[2]
                val replaceContent = match.groupValues[3]
                
                blocks.add(OutputParser.EditBlock(
                    filePath = filePath,
                    searchContent = searchContent,
                    replaceContent = replaceContent,
                    editType = OutputParser.EditType.SEARCH_REPLACE
                ))
            } catch (e: Exception) {
                logger.warn("Error parsing Claude Code search/replace block: ${e.message}", e)
            }
        }
    }
    
    private fun parseEditContent(editContent: String): OutputParser.EditBlock? {
        // This would parse Claude Code's specific edit format
        // Placeholder implementation - actual format would depend on Claude Code
        return null
    }
    
    override fun applyBlocks(blocks: List<OutputParser.EditBlock>): Map<String, Boolean> {
        modifiedFiles.clear()
        val results = mutableMapOf<String, Boolean>()
        
        for (block in blocks) {
            try {
                val success = when (block.editType) {
                    OutputParser.EditType.SEARCH_REPLACE -> applySearchReplaceBlock(block)
                    OutputParser.EditType.WHOLE_FILE -> replaceWholeFile(block.filePath, block.replaceContent)
                    OutputParser.EditType.UDIFF -> applyUdiffChange(block.filePath, block.replaceContent)
                    OutputParser.EditType.API_EDIT -> applyApiEdit(block)
                }
                
                if (success) {
                    modifiedFiles.add(block.filePath)
                }
                results[block.filePath] = success
                
            } catch (e: Exception) {
                logger.error("Error applying Claude Code edit block to file: ${block.filePath}", e)
                results[block.filePath] = false
            }
        }
        
        return results
    }
    
    private fun applySearchReplaceBlock(block: OutputParser.EditBlock): Boolean {
        // Similar to AiderOutputParser, but might handle Claude Code-specific nuances
        val absolutePath = resolveFilePath(block.filePath)
        val file = File(absolutePath)
        
        file.parentFile?.mkdirs()
        
        if (!file.exists()) {
            try {
                file.writeText(block.replaceContent)
                refreshVirtualFile(absolutePath)
                return true
            } catch (e: Exception) {
                logger.error("Failed to create file ${block.filePath}: ${e.message}", e)
                return false
            }
        } else {
            val content = file.readText()
            if (block.searchContent.isBlank()) {
                logger.warn("Empty search content for existing file: ${block.filePath}")
                return false
            } else {
                val normalizedContent = normalizeLineEndings(content)
                val normalizedSearchContent = normalizeLineEndings(block.searchContent)
                
                if (normalizedContent.contains(normalizedSearchContent)) {
                    val newContent = normalizedContent.replace(normalizedSearchContent, normalizeLineEndings(block.replaceContent))
                    val finalContent = if (content.contains("\r\n")) {
                        newContent.replace("\n", "\r\n")
                    } else {
                        newContent
                    }
                    
                    file.writeText(finalContent)
                    refreshVirtualFile(absolutePath)
                    return true
                } else {
                    logger.warn("Search content not found in file: ${block.filePath}")
                    showNotification("Search content not found in file: ${block.filePath}", NotificationType.ERROR)
                    return false
                }
            }
        }
    }
    
    private fun replaceWholeFile(filePath: String, newContent: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return false
            
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(newContent)
            }
            
            return true
        } catch (e: Exception) {
            logger.error("Error replacing file $filePath: ${e.message}", e)
            return false
        }
    }
    
    private fun applyUdiffChange(filePath: String, diffContent: String): Boolean {
        // Similar to Aider's udiff handling
        // Placeholder implementation
        return false
    }
    
    private fun applyApiEdit(block: OutputParser.EditBlock): Boolean {
        // Handle Claude Code's API-style edits
        // This would depend on the specific format Claude Code uses
        return false
    }
    
    override fun getModifiedFiles(): List<String> {
        return modifiedFiles.toList()
    }
    
    override fun clearModifiedFiles() {
        modifiedFiles.clear()
    }
    
    private fun normalizeLineEndings(text: String): String {
        return text.replace("\r\n", "\n")
    }
    
    private fun resolveFilePath(filePath: String): String {
        val projectPath = project.basePath ?: ""
        return if (File(filePath).isAbsolute) {
            filePath
        } else {
            Paths.get(projectPath, filePath).normalize().toString()
        }
    }
    
    private fun refreshVirtualFile(filePath: String) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
        virtualFile?.refresh(false, false)
    }
    
    private fun findOrCreateFile(filePath: String): VirtualFile? {
        val absolutePath = resolveFilePath(filePath)
        val file = File(absolutePath)
        
        file.parentFile?.mkdirs()
        
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                logger.error("Failed to create file $filePath: ${e.message}", e)
                return null
            }
        }
        
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    }
    
    private fun showNotification(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(project)
    }
}