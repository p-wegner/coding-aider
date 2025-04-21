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
    private val parser = SearchReplaceBlockParser(project)
    private val clipboardEditService = project.service<ClipboardEditService>()
    private val settings = AiderSettings.getInstance()
    
    /**
     * Processes the LLM response and applies any SEARCH/REPLACE blocks
     * @param llmResponse The response from the LLM
     * @return A summary of the changes made
     */
    fun processLlmResponse(llmResponse: String): String {
        logger.info("Processing LLM response for plugin-based edits")
        
        // First try to parse and apply SEARCH/REPLACE blocks
        val blocks = parser.parseBlocks(llmResponse)
        var changesApplied = 0
        
        if (blocks.isNotEmpty()) {
            logger.info("Found ${blocks.size} SEARCH/REPLACE blocks in LLM response")
            val results = parser.applyBlocks(blocks)
            changesApplied = results.count { it.value }
        } else {
            logger.info("No SEARCH/REPLACE blocks found in LLM response")
        }
        
        // If lenient edits is enabled, also try to process other edit formats
        if (settings.lenientEdits) {
            logger.info("Lenient edits enabled, trying to process other edit formats")
            val additionalChanges = clipboardEditService.processText(llmResponse)
            if (additionalChanges > 0) {
                logger.info("Applied $additionalChanges additional changes using other edit formats")
                changesApplied += additionalChanges
            }
        }
        
        // If no changes were applied and no blocks were found, return the original response
        if (changesApplied == 0 && blocks.isEmpty()) {
            return llmResponse
        }
        
        // Generate a summary of the changes
        val summary = buildString {
            appendLine("## Changes Applied")
            appendLine()
            
            if (blocks.isNotEmpty()) {
                val successCount = blocks.size
                val failureCount = blocks.size - changesApplied
                
                appendLine("**Summary:** Applied $changesApplied changes")
                if (settings.lenientEdits) {
                    appendLine("(Using both SEARCH/REPLACE blocks and other edit formats)")
                } else {
                    appendLine("(Using SEARCH/REPLACE blocks)")
                }
                appendLine()
                
                if (successCount > 0) {
                    appendLine("### Modified Files:")
                    parser.getModifiedFiles().sorted().forEach {
                        appendLine("- `$it`")
                    }
                    appendLine()
                }
            } else if (settings.lenientEdits && changesApplied > 0) {
                appendLine("**Summary:** Applied $changesApplied changes using other edit formats")
                appendLine()
                
                appendLine("### Modified Files:")
                clipboardEditService.getModifiedFiles().sorted().forEach {
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
