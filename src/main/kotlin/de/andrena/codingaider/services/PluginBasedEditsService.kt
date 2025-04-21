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
        val modifiedFilesByBlocks = mutableSetOf<String>()
        
        if (blocks.isNotEmpty()) {
            logger.info("Found ${blocks.size} SEARCH/REPLACE blocks in LLM response")
            val results = parser.applyBlocks(blocks)
            changesApplied = results.count { it.value }
            
            // Track which files were modified by SEARCH/REPLACE blocks
            modifiedFilesByBlocks.addAll(parser.getModifiedFiles())
        } else {
            logger.info("No SEARCH/REPLACE blocks found in LLM response")
        }
        
        // If lenient edits is enabled, also try to process other edit formats
        // but only for files not already modified by SEARCH/REPLACE blocks
        if (settings.lenientEdits) {
            logger.info("Lenient edits enabled, trying to process other edit formats")
            
            // Clear any previous state in the clipboard service
            clipboardEditService.clearModifiedFiles()
            
            // Process the text with the clipboard service
            val additionalChanges = clipboardEditService.processText(llmResponse)
            
            // Get the list of files modified by the clipboard service
            val clipboardModifiedFiles = clipboardEditService.getModifiedFiles()
            
            // Count only changes to files not already modified by SEARCH/REPLACE blocks
            val uniqueAdditionalChanges = clipboardModifiedFiles.count { !modifiedFilesByBlocks.contains(it) }
            
            if (uniqueAdditionalChanges > 0) {
                logger.info("Applied $uniqueAdditionalChanges additional changes using other edit formats")
                changesApplied += uniqueAdditionalChanges
            }
        }
        
        // If no changes were applied and no blocks were found, return the original response
        if (changesApplied == 0 && blocks.isEmpty()) {
            return llmResponse
        }
        
        // Generate a concise summary of the changes
        val summary = buildString {
            appendLine("## Changes Applied")
            appendLine()
            
            // Simple count of changes
            appendLine("**Applied $changesApplied changes to ${parser.getModifiedFiles().size + clipboardEditService.getModifiedFiles().filter { !modifiedFilesByBlocks.contains(it) }.size} files**")
            
            // List modified files in a compact format
            val allModifiedFiles = (parser.getModifiedFiles() + 
                clipboardEditService.getModifiedFiles().filter { !modifiedFilesByBlocks.contains(it) }).sorted()
            
            if (allModifiedFiles.isNotEmpty()) {
                appendLine("**Files:** ${allModifiedFiles.joinToString(", ") { "`$it`" }}")
                appendLine()
            }
            
            appendLine("## Original LLM Response")
            appendLine()
            appendLine(llmResponse)
        }
        
        return summary
    }
}
