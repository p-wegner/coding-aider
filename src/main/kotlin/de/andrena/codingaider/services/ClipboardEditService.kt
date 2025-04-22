package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.SearchReplaceBlockParser

/**
 * Service for handling clipboard-based edits
 * Delegates to SearchReplaceBlockParser for actual parsing and applying of edits
 */
@Service(Service.Level.PROJECT)
class ClipboardEditService(private val project: Project) {
    private val parser = project.service<SearchReplaceBlockParser>()
    
    /**
     * Process text that may contain multiple edit blocks in various formats
     * @return The number of successfully applied changes
     */
    fun processText(text: String): Int {
        // Clear the list of modified files
        clearModifiedFiles()
        
        // Parse all edit blocks from the text
        val blocks = parser.parseBlocks(text)
        
        // Apply the blocks and count successful changes
        val results = parser.applyBlocks(blocks)
        
        return results.count { it.value }
    }

    /**
     * Get the list of files that were modified by the last processText call
     * @return List of modified file paths
     */
    fun getModifiedFiles(): List<String> {
        return parser.getModifiedFiles()
    }

    /**
     * Clear the list of modified files
     */
    fun clearModifiedFiles() {
        parser.clearModifiedFiles()
    }
}
