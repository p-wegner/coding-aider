package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.SearchReplaceBlockParser

/**
 * Service for handling plugin-based edits
 */
@Service(Service.Level.PROJECT)
class PluginBasedEditsService(private val project: Project) {
    private val logger = Logger.getInstance(PluginBasedEditsService::class.java)
    private val parser = project.service<SearchReplaceBlockParser>()
    private val settings = AiderSettings.getInstance()
    private val autoCommitService by lazy { project.service<AutoCommitService>() }
    
    /**
     * Processes the LLM response and applies any edit blocks
     * @param llmResponse The response from the LLM
     * @return A summary of the changes made
     */
    fun processLlmResponse(llmResponse: String): String {
        logger.info("Processing LLM response for plugin-based edits")
        
        // Parse all edit blocks from the response
        val blocks = parser.parseBlocks(llmResponse)
        
        if (blocks.isNotEmpty()) {
            logger.info("Found ${blocks.size} edit blocks in LLM response")
            
            // Apply the blocks
            val results = parser.applyBlocks(blocks)
            val changesApplied = results.count { it.value }
            
            // If no changes were applied, return the original response
            if (changesApplied == 0) {
                return llmResponse
            }
            
            // Generate a concise summary of the changes
            val modifiedFiles = parser.getModifiedFiles().sorted()
            
            // Try to auto-commit the changes if enabled
            val commitSuccessful = autoCommitService.tryAutoCommit(llmResponse, modifiedFiles)
            
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
                    val commitMessage = autoCommitService.getLastCommitMessage() ?: "Unknown commit message"
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
