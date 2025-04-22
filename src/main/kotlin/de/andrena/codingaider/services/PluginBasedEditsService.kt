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
            
            val summary = buildString {
                appendLine("## Original LLM Response")
                appendLine()
                appendLine(llmResponse)
                appendLine()
                
                if (modifiedFiles.isNotEmpty()) {
                    appendLine("**Files:** ${modifiedFiles.joinToString(", ") { "`$it`" }}")
                    appendLine()
                }
                
                appendLine("## Changes Applied")
                appendLine()
                appendLine("**Applied $changesApplied changes to ${modifiedFiles.size} files**")
            }
            
            return summary
        } else {
            logger.info("No edit blocks found in LLM response")
            return llmResponse
        }
    }
}
