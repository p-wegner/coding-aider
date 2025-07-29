package de.andrena.codingaider.providers

/**
 * Interface for parsing AI provider output into actionable edit blocks
 */
interface OutputParser {
    val provider: AIProvider
    
    /**
     * Represents a parsed edit instruction from the AI provider
     */
    data class EditBlock(
        val filePath: String,
        val language: String = "",
        val searchContent: String = "",
        val replaceContent: String = "",
        val editType: EditType = EditType.SEARCH_REPLACE
    )
    
    /**
     * Types of edits that can be parsed
     */
    enum class EditType {
        SEARCH_REPLACE,
        WHOLE_FILE,
        UDIFF,
        API_EDIT // For providers that use API-based edits (like Claude Code)
    }
    
    /**
     * Parses the AI provider's response text to extract edit instructions
     * @param text The response text from the AI provider
     * @return List of parsed edit blocks
     */
    fun parseBlocks(text: String): List<EditBlock>
    
    /**
     * Applies the parsed edit blocks to the actual files
     * @param blocks List of edit blocks to apply
     * @return Map of file paths to success/failure status
     */
    fun applyBlocks(blocks: List<EditBlock>): Map<String, Boolean>
    
    /**
     * Gets the list of files that were modified by the last applyBlocks call
     * @return List of modified file paths
     */
    fun getModifiedFiles(): List<String>
    
    /**
     * Clears the list of modified files
     */
    fun clearModifiedFiles()
}