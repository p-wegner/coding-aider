package de.andrena.codingaider.providers.parsers

import com.intellij.openapi.project.Project
import de.andrena.codingaider.providers.AIProvider
import de.andrena.codingaider.providers.OutputParser
import de.andrena.codingaider.utils.SearchReplaceBlockParser

/**
 * Output parser for Aider that wraps the existing SearchReplaceBlockParser
 */
class AiderOutputParser(private val project: Project) : OutputParser {
    override val provider: AIProvider = AIProvider.AIDER
    
    private val searchReplaceBlockParser = SearchReplaceBlockParser(project)
    
    override fun parseBlocks(text: String): List<OutputParser.EditBlock> {
        val aiderBlocks = searchReplaceBlockParser.parseBlocks(text)
        
        // Convert SearchReplaceBlockParser.EditBlock to OutputParser.EditBlock
        return aiderBlocks.map { aiderBlock ->
            OutputParser.EditBlock(
                filePath = aiderBlock.filePath,
                language = aiderBlock.language,
                searchContent = aiderBlock.searchContent,
                replaceContent = aiderBlock.replaceContent,
                editType = convertEditType(aiderBlock.editType)
            )
        }
    }
    
    override fun applyBlocks(blocks: List<OutputParser.EditBlock>): Map<String, Boolean> {
        // Convert OutputParser.EditBlock back to SearchReplaceBlockParser.EditBlock
        val aiderBlocks = blocks.map { block ->
            SearchReplaceBlockParser.EditBlock(
                filePath = block.filePath,
                language = block.language,
                searchContent = block.searchContent,
                replaceContent = block.replaceContent,
                editType = convertEditTypeBack(block.editType)
            )
        }
        
        return searchReplaceBlockParser.applyBlocks(aiderBlocks)
    }
    
    override fun getModifiedFiles(): List<String> {
        return searchReplaceBlockParser.getModifiedFiles()
    }
    
    override fun clearModifiedFiles() {
        searchReplaceBlockParser.clearModifiedFiles()
    }
    
    private fun convertEditType(aiderType: SearchReplaceBlockParser.EditType): OutputParser.EditType {
        return when (aiderType) {
            SearchReplaceBlockParser.EditType.SEARCH_REPLACE -> OutputParser.EditType.SEARCH_REPLACE
            SearchReplaceBlockParser.EditType.WHOLE_FILE -> OutputParser.EditType.WHOLE_FILE
            SearchReplaceBlockParser.EditType.UDIFF -> OutputParser.EditType.UDIFF
        }
    }
    
    private fun convertEditTypeBack(providerType: OutputParser.EditType): SearchReplaceBlockParser.EditType {
        return when (providerType) {
            OutputParser.EditType.SEARCH_REPLACE -> SearchReplaceBlockParser.EditType.SEARCH_REPLACE
            OutputParser.EditType.WHOLE_FILE -> SearchReplaceBlockParser.EditType.WHOLE_FILE
            OutputParser.EditType.UDIFF -> SearchReplaceBlockParser.EditType.UDIFF
            OutputParser.EditType.API_EDIT -> SearchReplaceBlockParser.EditType.SEARCH_REPLACE // Fallback
        }
    }
}