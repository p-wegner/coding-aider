package de.andrena.codingaider.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Paths

/**
 * Parses and applies SEARCH/REPLACE blocks from LLM responses
 */
class SearchReplaceBlockParser(private val project: Project) {
    private val logger = Logger.getInstance(SearchReplaceBlockParser::class.java)
    private val modifiedFiles = mutableSetOf<String>()

    /**
     * Represents a parsed SEARCH/REPLACE block
     */
    data class SearchReplaceBlock(
        val filePath: String,
        val language: String,
        val searchContent: String,
        val replaceContent: String
    )

    /**
     * Parses the LLM response text to extract SEARCH/REPLACE blocks
     * @param text The LLM response text
     * @return List of parsed SearchReplaceBlock objects
     */
    fun parseBlocks(text: String): List<SearchReplaceBlock> {
        val blocks = mutableListOf<SearchReplaceBlock>()
        
        // Match both standard and quadruple backtick formats
        val standardRegex = """(?m)^([^\n]+)\n```([^\n]*)\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val quadrupleRegex = """(?m)^([^\n]+)\n````([^\n]*)\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n````""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // Process standard format
        standardRegex.findAll(text).forEach { match ->
            val (filePath, language, searchContent, replaceContent) = match.destructured
            blocks.add(
                SearchReplaceBlock(
                    filePath = filePath.trim(),
                    language = language.trim(),
                    searchContent = searchContent,
                    replaceContent = replaceContent
                )
            )
        }
        
        // Process quadruple backtick format
        quadrupleRegex.findAll(text).forEach { match ->
            val (filePath, language, searchContent, replaceContent) = match.destructured
            blocks.add(
                SearchReplaceBlock(
                    filePath = filePath.trim(),
                    language = language.trim(),
                    searchContent = searchContent,
                    replaceContent = replaceContent
                )
            )
        }
        
        return blocks
    }

    /**
     * Represents the result of applying a search/replace block
     */
    data class BlockResult(
        val block: SearchReplaceBlock,
        val success: Boolean,
        val message: String? = null
    )

    /**
     * Get the list of files that were modified by the last applyBlocks call
     * @return List of modified file paths
     */
    fun getModifiedFiles(): List<String> {
        return modifiedFiles.toList()
    }

    /**
     * Applies the SEARCH/REPLACE blocks to the files
     * @param blocks List of SearchReplaceBlock objects to apply
     * @return Map of file paths to success/failure status
     */
    fun applyBlocks(blocks: List<SearchReplaceBlock>): Map<String, Boolean> {
        // Clear the list of modified files
        modifiedFiles.clear()
        val results = mutableListOf<BlockResult>()
        
        for (block in blocks) {
            try {
                val absolutePath = resolveFilePath(block.filePath)
                val file = File(absolutePath)
                
                // Create parent directories if they don't exist
                file.parentFile?.mkdirs()
                
                if (!file.exists() && block.searchContent.isBlank()) {
                    // Creating a new file
                    file.writeText(block.replaceContent)
                    refreshVirtualFile(absolutePath)
                    modifiedFiles.add(block.filePath)
                    results.add(BlockResult(block, true, "File created successfully"))
                } else if (file.exists()) {
                    // Modifying an existing file
                    val content = file.readText()
                    if (block.searchContent.isBlank()) {
                        // Empty search content means append to file
                        file.writeText(content + block.replaceContent)
                        refreshVirtualFile(absolutePath)
                        modifiedFiles.add(block.filePath)
                        results.add(BlockResult(block, true, "Content appended to file"))
                    } else {
                        // Replace content in file
                        val newContent = content.replace(block.searchContent, block.replaceContent)
                        if (content != newContent) {
                            file.writeText(newContent)
                            refreshVirtualFile(absolutePath)
                            modifiedFiles.add(block.filePath)
                            results.add(BlockResult(block, true, "Content replaced successfully"))
                        } else {
                            val message = "Search content not found in file: ${block.filePath}"
                            logger.warn(message)
                            results.add(BlockResult(block, false, message))
                        }
                    }
                } else {
                    val message = "File not found and search content is not empty: ${block.filePath}"
                    logger.warn(message)
                    results.add(BlockResult(block, false, message))
                }
            } catch (e: Exception) {
                val message = "Error applying block to file: ${block.filePath} - ${e.message}"
                logger.error(message, e)
                results.add(BlockResult(block, false, message))
            }
        }
        
        // Convert the list of BlockResult to the expected Map<String, Boolean> format
        return results.associate { it.block.filePath to it.success }
    }
    
    /**
     * Resolves a relative file path to an absolute path
     * @param filePath The file path to resolve
     * @return The absolute file path
     */
    private fun resolveFilePath(filePath: String): String {
        val projectPath = project.basePath ?: ""
        return if (File(filePath).isAbsolute) {
            filePath
        } else {
            Paths.get(projectPath, filePath).normalize().toString()
        }
    }
    
    /**
     * Refreshes the virtual file in the IDE
     * @param filePath The absolute path of the file to refresh
     */
    private fun refreshVirtualFile(filePath: String) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
        virtualFile?.refresh(false, false)
    }
}
