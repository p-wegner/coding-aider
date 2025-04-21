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
        val regex = """(?m)^([^\n]+)\n```([^\n]*)\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        val matches = regex.findAll(text)
        for (match in matches) {
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
     * Applies the SEARCH/REPLACE blocks to the files
     * @param blocks List of SearchReplaceBlock objects to apply
     * @return Map of file paths to success/failure status
     */
    fun applyBlocks(blocks: List<SearchReplaceBlock>): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        
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
                    results[block.filePath] = true
                } else if (file.exists()) {
                    // Modifying an existing file
                    val content = file.readText()
                    if (block.searchContent.isBlank()) {
                        // Empty search content means append to file
                        file.writeText(content + block.replaceContent)
                        refreshVirtualFile(absolutePath)
                        results[block.filePath] = true
                    } else {
                        // Replace content in file
                        val newContent = content.replace(block.searchContent, block.replaceContent)
                        if (content != newContent) {
                            file.writeText(newContent)
                            refreshVirtualFile(absolutePath)
                            results[block.filePath] = true
                        } else {
                            logger.warn("Search content not found in file: ${block.filePath}")
                            results[block.filePath] = false
                        }
                    }
                } else {
                    logger.warn("File not found and search content is not empty: ${block.filePath}")
                    results[block.filePath] = false
                }
            } catch (e: Exception) {
                logger.error("Error applying block to file: ${block.filePath}", e)
                results[block.filePath] = false
            }
        }
        
        return results
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
