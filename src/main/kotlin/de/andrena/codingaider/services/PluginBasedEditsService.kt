package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.SearchReplaceBlockParser

/**
 * Service for handling plugin-based edits
 */
@Service(Service.Level.PROJECT)
class PluginBasedEditsService(private val project: Project) {
    private val logger = Logger.getInstance(PluginBasedEditsService::class.java)
    private val parser = SearchReplaceBlockParser(project)
    
    /**
     * Processes the LLM response and applies any SEARCH/REPLACE blocks
     * @param llmResponse The response from the LLM
     * @return A summary of the changes made
     */
    fun processLlmResponse(llmResponse: String): String {
        logger.info("Processing LLM response for plugin-based edits")
        
        val blocks = parser.parseBlocks(llmResponse)
        if (blocks.isEmpty()) {
            logger.info("No SEARCH/REPLACE blocks found in LLM response")
            return llmResponse
        }
        
        logger.info("Found ${blocks.size} SEARCH/REPLACE blocks in LLM response")
        
        val results = parser.applyBlocks(blocks)
        
        // Generate a summary of the changes
        val summary = buildString {
            appendLine("## Changes Applied")
            appendLine()
            
            val successCount = results.count { it.value }
            val failureCount = results.count { !it.value }
            
            appendLine("**Summary:** Applied $successCount changes, $failureCount failed")
            appendLine()
            
            if (successCount > 0) {
                appendLine("### Successfully Modified Files:")
                results.filter { it.value }.keys.sorted().forEach { 
                    appendLine("- `$it`") 
                }
                appendLine()
            }
            
            if (failureCount > 0) {
                appendLine("### Failed to Modify Files:")
                results.filter { !it.value }.keys.sorted().forEach { 
                    appendLine("- `$it`") 
                }
                appendLine()
            }
            
            appendLine("## Original LLM Response")
            appendLine()
            appendLine(llmResponse)
        }
        
        return summary
    }
}
