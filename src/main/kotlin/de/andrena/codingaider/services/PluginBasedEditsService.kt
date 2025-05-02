package de.andrena.codingaider.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.SearchReplaceBlockParser
import java.io.File

/**
 * Service for handling plugin-based edits
 */
@Service(Service.Level.PROJECT)
class PluginBasedEditsService(private val project: Project) {
    private val logger = Logger.getInstance(PluginBasedEditsService::class.java)
    private val settings = AiderSettings.getInstance()

    /**
     * Processes the LLM response and applies any edit blocks
     * @param llmResponse The response from the LLM
     * @return A summary of the changes made
     */
    fun processLlmResponse(llmResponse: String): String {
        logger.info("Processing LLM response for plugin-based edits")
        
        // Parse all edit blocks from the response
        val blocks = project.service<SearchReplaceBlockParser>().parseBlocks(llmResponse)
        
        if (blocks.isNotEmpty()) {
            logger.info("Found ${blocks.size} edit blocks in LLM response")
            
            // Apply the blocks
            val results = project.service<SearchReplaceBlockParser>().applyBlocks(blocks)
            val changesApplied = results.count { it.value }
            
            // If no changes were applied, return the original response
            if (changesApplied == 0) {
                return llmResponse
            }
            
            // Generate a concise summary of the changes
            val modifiedFiles = project.service<SearchReplaceBlockParser>().getModifiedFiles().sorted()
            
            // Try to auto-commit the changes if enabled
            // Ensure file I/O operations are completed before attempting to commit
            val commitSuccessful = if (modifiedFiles.isNotEmpty()) {
                // Force refresh of files to ensure they're up-to-date in the VFS
                ApplicationManager.getApplication().invokeAndWait {
                    modifiedFiles.forEach { filePath ->
                        val file = File(filePath)
                        if (file.exists()) {
                            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)?.refresh(false, false)
                        }
                    }
                }
                
                // Small delay to ensure file system operations are complete
                Thread.sleep(100)
                
                // Now try to commit
                project.service<AutoCommitService>().tryAutoCommit(llmResponse, modifiedFiles)
            } else {
                false
            }
            
            val summary = buildString {
                appendLine("## Original LLM Response")
                appendLine()
                appendLine(llmResponse)
                appendLine()
                
                appendLine("## Changes Applied")
                appendLine()
                if (modifiedFiles.isNotEmpty()) {
                    appendLine("**Applied $changesApplied changes to ${modifiedFiles.size} files:**")
                    appendLine()
                    modifiedFiles.forEach { file ->
                        appendLine("- `$file`")
                    }
                    appendLine()
                } else {
                    appendLine("**No files were modified**")
                    appendLine()
                }
                
                if (commitSuccessful) {
                    val commitMessage = project.service<AutoCommitService>().getLastCommitMessage() ?: "Unknown commit message"
                    appendLine("## Git Commit")
                    appendLine()
                    appendLine("**Changes automatically committed to Git repository**")
                    appendLine()
                    appendLine("```")
                    appendLine(commitMessage)
                    appendLine("```")
                } else if (settings.autoCommitAfterEdits && modifiedFiles.isNotEmpty()) {
                    appendLine("## Git Commit")
                    appendLine()
                    appendLine("**Auto-commit was enabled but failed**")
                    appendLine()
                    appendLine("Possible reasons:")
                    appendLine("- No commit message found in LLM response")
                    appendLine("- Git repository issues")
                    appendLine("- Missing dependencies (prompt augmentation, commit message block)")
                }
            }
            
            return summary
        } else {
            logger.info("No edit blocks found in LLM response")
            return llmResponse
        }
    }
}
