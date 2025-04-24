package de.andrena.codingaider.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import java.io.File
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Parses and applies various edit formats from LLM responses
 */
@Service(Service.Level.PROJECT)
class SearchReplaceBlockParser(private val project: Project) {
    private val logger = Logger.getInstance(SearchReplaceBlockParser::class.java)
    private val modifiedFiles = mutableSetOf<String>()

    companion object {
        // --- Refined Search/Replace Regexes ---

        // Quadruple backtick format: filepath\n````lang?\nSEARCH\n...\nREPLACE\n````
        // Handles optional language, CRLF/LF, DOT_MATCHES_ALL for content, $ anchor.
        private val QUADRUPLE_REGEX = """(?m)^([^\r\n]+)\r?\n````([^\r\n]*)\r?\n<<<<<<< SEARCH\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n````$""".toRegex(RegexOption.DOT_MATCHES_ALL)

        // Triple backtick format with mandatory language: filepath\n```lang\nSEARCH\n...\nREPLACE\n```
        // Handles mandatory language ([^\r\n]+), CRLF/LF, DOT_MATCHES_ALL, $ anchor.
        private val LANGUAGE_TRIPLE_REGEX = """(?m)^([^\r\n]+)\r?\n```([^\r\n]+)\r?\n<<<<<<< SEARCH\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n```$""".toRegex(RegexOption.DOT_MATCHES_ALL)

        // Triple backtick format without language: filepath\n```\nSEARCH\n...\nREPLACE\n```
        // Handles no language, CRLF/LF, DOT_MATCHES_ALL, $ anchor.
        private val SIMPLE_TRIPLE_REGEX = """(?m)^([^\r\n]+)\r?\n```\r?\n<<<<<<< SEARCH\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n```$""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // New file block format: filepath\n```lang?\n<<<<<<< SEARCH\n=======\ncontent\n>>>>>>> REPLACE\n```
        // This format is specifically for creating new files with empty SEARCH section
        private val NEW_FILE_BLOCK_REGEX = """(?m)^([^\r\n]+)\r?\n```([^\r\n]*)\r?\n<<<<<<< SEARCH\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n```$""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // Quadruple backtick version of new file block format
        private val QUADRUPLE_NEW_FILE_REGEX = """(?m)^([^\r\n]+)\r?\n````([^\r\n]*)\r?\n<<<<<<< SEARCH\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n````$""".toRegex(RegexOption.DOT_MATCHES_ALL)

        // --- Other Formats ---

        // Pattern to identify the instruction prompt (remains the same)
        private val INSTRUCTION_PROMPT_PATTERN = Pattern.compile(
            """When making code changes, please format them as SEARCH/REPLACE blocks using this format:[\s\S]*?Make your changes precise and minimal\.""",
            Pattern.MULTILINE
        )

        // Alternative format with fenced blocks (diff-fenced)
        private val DIFF_FENCED_REGEX = """```\n(.+?)\n<<<<<<< SEARCH\n([\s\S]*?)=======\n([\s\S]*?)>>>>>>> REPLACE\n```""".toRegex(RegexOption.MULTILINE)

        // Whole file replacement pattern - exclude matches containing SEARCH/REPLACE blocks
        // Note: The negative lookahead (?!.*?<<<<<<< SEARCH) helps, but we add an extra check later
        private val WHOLE_REGEX = """(.+?)\n```(?:\w*)\n(?!.*?<<<<<<< SEARCH)([\s\S]*?)\n```""".toRegex(RegexOption.MULTILINE)

        // Unified diff format pattern
        private val UDIFF_REGEX = """```diff\n--- (.+?)\n\+\+\+ \1\n@@ .* @@\n([\s\S]*?)\n```""".toRegex(RegexOption.MULTILINE)
    }

    /**
     * Represents a parsed edit block
     */
    data class EditBlock(
        val filePath: String,
        val language: String = "",
        val searchContent: String = "",
        val replaceContent: String = "",
        val editType: EditType = EditType.SEARCH_REPLACE
    )

    enum class EditType {
        SEARCH_REPLACE,
        WHOLE_FILE,
        UDIFF
    }

    /**
     * Parses the LLM response text to extract all supported edit formats
     * @param text The LLM response text
     * @return List of parsed EditBlock objects
     */
    fun parseBlocks(text: String): List<EditBlock> {
        val blocks = mutableListOf<EditBlock>()
        
        // Filter out the instruction prompt if it exists in the text
        var remainingText = filterInstructionPrompt(text)
        val processedRanges = mutableListOf<IntRange>()

        // Function to check if a range overlaps with already processed ranges
        fun isProcessed(range: IntRange): Boolean {
            return processedRanges.any { it.first <= range.first && it.last >= range.last || range.first <= it.first && range.last >= it.last || range.first in it || range.last in it }
        }

        // Function to add a processed range
        fun addProcessedRange(range: IntRange) {
            processedRanges.add(range)
        }

        // Define the order of regex patterns to try (most specific SEARCH/REPLACE first)
        val regexPatterns = listOf(
            QUADRUPLE_NEW_FILE_REGEX,  // ````lang? \n <<< SEARCH \n ======= \n content \n >>> REPLACE \n ````
            NEW_FILE_BLOCK_REGEX,      // ```lang \n <<< SEARCH \n ======= \n content \n >>> REPLACE \n ```
            QUADRUPLE_REGEX,           // ````lang? ... ````
            LANGUAGE_TRIPLE_REGEX,     // ```lang ... ```
            SIMPLE_TRIPLE_REGEX,       // ``` ... ```
            // --- Other formats ---
            DIFF_FENCED_REGEX,         // ``` \n path \n <<< ... >>> \n ```
            WHOLE_REGEX,               // path \n ``` content ```
            UDIFF_REGEX                // ```diff ... ```
        )

        // Iterate through patterns and find matches
        regexPatterns.forEach { regex ->
            // Ensure we are calling findAll on Kotlin Regex
            val currentRegex: Regex = when (regex) {
                is Regex -> regex
                // This case should ideally not happen if the list is correctly defined
                else -> {
                    logger.warn("Unexpected type in regexPatterns list: ${regex::class.simpleName}")
                    return@forEach // Skip this pattern
                }
            }

            currentRegex.findAll(remainingText).forEach { match ->
                if (!isProcessed(match.range)) {
                    try {
                        when (currentRegex) {
                            QUADRUPLE_REGEX -> {
                                // Groups: 1=filePath, 2=language(optional), 3=search, 4=replace
                                val (filePath, language, searchContent, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = language.trim(), // Language might be empty
                                    searchContent = searchContent,
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            LANGUAGE_TRIPLE_REGEX -> {
                                // Groups: 1=filePath, 2=language(mandatory), 3=search, 4=replace
                                val (filePath, language, searchContent, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = language.trim(), // Language is mandatory here
                                    searchContent = searchContent,
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            SIMPLE_TRIPLE_REGEX -> {
                                // Groups: 1=filePath, 2=search, 3=replace (No language group)
                                val (filePath, searchContent, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = "", // No language specified
                                    searchContent = searchContent,
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            NEW_FILE_BLOCK_REGEX, QUADRUPLE_NEW_FILE_REGEX -> {
                                // Groups: 1=filePath, 2=language, 3=replaceContent (empty search content)
                                val (filePath, language, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = language.trim(),
                                    searchContent = "", // Empty search content for new file
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            // This case should not be reached as these regexes are already handled above
                            // This is a fallback in case something goes wrong with the pattern matching
                            else -> {
                                logger.warn("Unexpected regex match: ${currentRegex.pattern}")
                            }
                            DIFF_FENCED_REGEX -> {
                                // Groups: 1=filePath, 2=search, 3=replace
                                if (match.groups.size > 3) { // Check group count
                                    blocks.add(EditBlock(
                                        filePath = match.groupValues[1].trim(),
                                        searchContent = match.groupValues[2],
                                        replaceContent = match.groupValues[3],
                                        editType = EditType.SEARCH_REPLACE
                                    ))
                                    addProcessedRange(match.range)
                                } else {
                                     logger.warn("DIFF_FENCED_REGEX match failed group extraction: ${match.value}")
                                }
                            }
                            WHOLE_REGEX -> {
                                // Groups: 1=filePath, 2=replaceContent
                                // Extra check: Avoid matching blocks already identified as SEARCH_REPLACE or UDIFF
                                val content = match.value
                                if (!content.contains("<<<<<<< SEARCH") && !content.startsWith("```diff")) {
                                     if (match.groups.size > 2) {
                                        blocks.add(EditBlock(
                                            filePath = match.groupValues[1].trim(),
                                            replaceContent = match.groupValues[2],
                                            editType = EditType.WHOLE_FILE
                                        ))
                                        addProcessedRange(match.range)
                                     } else {
                                         logger.warn("WHOLE_REGEX match failed group extraction: ${match.value}")
                                     }
                                }
                            }
                            UDIFF_REGEX -> {
                                // Groups: 1=filePath, 2=diffContent
                                if (match.groups.size > 2) {
                                    blocks.add(EditBlock(
                                        filePath = match.groupValues[1].trim(),
                                        replaceContent = match.groupValues[2], // The actual diff content
                                        editType = EditType.UDIFF
                                    ))
                                    addProcessedRange(match.range)
                                } else {
                                    logger.warn("UDIFF_REGEX match failed group extraction: ${match.value}")
                                }
                            }
                            else -> {
                                // Should not happen if all regexes in the list are handled
                                logger.warn("Unhandled regex pattern in when block: ${currentRegex.pattern}")
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Error processing match for regex ${currentRegex.pattern}: ${e.message}", e)
                    }
                }
            }
        }

        return blocks
    }
    
    /**
     * Filters out the instruction prompt from the text
     * @param text The text to filter
     * @return The filtered text
     */
    private fun filterInstructionPrompt(text: String): String {
        // Get the instruction prompt from AiderDefaults
        val instructionPrompt = de.andrena.codingaider.settings.AiderDefaults.PLUGIN_BASED_EDITS_INSTRUCTION.trim()
        
        // First try direct string replacement (most efficient)
        var filteredText = text.replace(instructionPrompt, "")
        
        // If that doesn't work well (e.g., if there are whitespace differences),
        // use regex pattern matching as a fallback
        val matcher = INSTRUCTION_PROMPT_PATTERN.matcher(filteredText)
        if (matcher.find()) {
            filteredText = matcher.replaceAll("")
        }
        
        return filteredText
    }

    /**
     * Represents the result of applying an edit block
     */
    data class BlockResult(
        val block: EditBlock,
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
     * Clear the list of modified files
     */
    fun clearModifiedFiles() {
        modifiedFiles.clear()
    }

    /**
     * Applies the edit blocks to the files
     * @param blocks List of EditBlock objects to apply
     * @return Map of file paths to success/failure status
     */
    fun applyBlocks(blocks: List<EditBlock>): Map<String, Boolean> {
        // Clear the list of modified files
        modifiedFiles.clear()
        val results = mutableListOf<BlockResult>()
        
        for (block in blocks) {
            try {
                val success = when (block.editType) {
                    EditType.SEARCH_REPLACE -> applySearchReplaceBlock(block)
                    EditType.WHOLE_FILE -> replaceWholeFile(block.filePath, block.replaceContent)
                    EditType.UDIFF -> applyUdiffChange(block.filePath, block.replaceContent)
                }
                
                if (success) {
                    modifiedFiles.add(block.filePath)
                    results.add(BlockResult(block, true, "Changes applied successfully"))
                } else {
                    results.add(BlockResult(block, false, "Failed to apply changes"))
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
     * Applies a search/replace block to a file
     */
    private fun applySearchReplaceBlock(block: EditBlock): Boolean {
        val absolutePath = resolveFilePath(block.filePath)
        val file = File(absolutePath)
        
        // Create parent directories if they don't exist
        file.parentFile?.mkdirs()
        
        if (!file.exists()) {
            // File does not exist: Create it if search block is empty or effectively empty
            // This is the standard behavior for creating new files with SEARCH/REPLACE.
            logger.info("File does not exist, creating: ${block.filePath}")
            try {
                file.writeText(block.replaceContent)
                refreshVirtualFile(absolutePath)
                logger.info("Successfully created file: ${block.filePath}")
                return true
            } catch (e: Exception) {
                val message = "Failed to create file ${block.filePath}: ${e.message}"
                logger.error(message, e)
                showNotification(message, NotificationType.ERROR)
                return false
            }
        } else {
            // File exists: Modifying an existing file
            val content = file.readText()
            if (block.searchContent.isBlank()) {
                // Standard Aider behavior: Empty search block is for creating NEW files.
                // Applying an empty search block to an EXISTING file is ambiguous/undefined.
                val message = "Attempted to apply SEARCH/REPLACE block with empty SEARCH section to existing file: ${block.filePath}. This operation is typically for creating new files. Skipping."
                logger.warn(message)
                // Optionally show notification, but might be noisy if LLM makes mistakes often.
                // showNotification(message, NotificationType.WARNING)
                return false // Indicate failure/skip
            } else {
                // Normalize line endings for comparison
                val normalizedContent = normalizeLineEndings(content)
                val normalizedSearchContent = normalizeLineEndings(block.searchContent)
                
                // First try exact match
                if (normalizedContent.contains(normalizedSearchContent)) {
                    // Use the normalized content for replacement
                    val newContent = normalizedContent.replace(normalizedSearchContent, normalizeLineEndings(block.replaceContent))
                    // Preserve original line endings when writing back
                    val finalContent = if (content.contains("\r\n")) {
                        newContent.replace("\n", "\r\n")
                    } else {
                        newContent
                    }
                    
                    file.writeText(finalContent)
                    refreshVirtualFile(absolutePath)
                    return true
                } else {
                    val message = "Search content not found in file: ${block.filePath}"
                    logger.warn(message)
                    showNotification(message, NotificationType.ERROR)
                    return false
                }
            }
        }
    }
    
    /**
     * Normalizes line endings to \n for consistent comparison
     */
    private fun normalizeLineEndings(text: String): String {
        return text.replace("\r\n", "\n")
    }

    /**
     * Replaces the entire content of a file
     */
    private fun replaceWholeFile(filePath: String, newContent: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            // Preserve the original line endings of the file if it exists
            val finalContent = if (file.exists()) {
                val existingContent = file.readText()
                if (existingContent.contains("\r\n")) {
                    // If the file uses CRLF, ensure the new content does too
                    normalizeLineEndings(newContent).replace("\n", "\r\n")
                } else {
                    // Otherwise use LF
                    normalizeLineEndings(newContent)
                }
            } else {
                // For new files, use the platform default
                newContent
            }
            
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(finalContent)
            }
            
            return true
        } catch (e: Exception) {
            val message = "Error replacing file $filePath: ${e.message}"
            logger.error(message, e)
            showNotification(message, NotificationType.ERROR)
            return false
        }
    }

    /**
     * Applies a unified diff to a file
     */
    private fun applyUdiffChange(filePath: String, diffContent: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            val fileContent = document.text
            // Determine the line ending style of the original file
            val lineEnding = if (fileContent.contains("\r\n")) "\r\n" else "\n"
            
            // Normalize line endings for processing
            val normalizedContent = normalizeLineEndings(fileContent)
            val lines = normalizedContent.lines().toMutableList()
            
            // Process diff lines (normalize diff content too)
            var currentLine = 0
            val diffLines = normalizeLineEndings(diffContent).lines()
            
            for (diffLine in diffLines) {
                when {
                    diffLine.startsWith("+") && !diffLine.startsWith("+++") -> {
                        // Add line
                        val lineContent = diffLine.substring(1)
                        lines.add(currentLine, lineContent)
                        currentLine++
                    }
                    diffLine.startsWith("-") && !diffLine.startsWith("---") -> {
                        // Remove line
                        if (currentLine < lines.size) {
                            lines.removeAt(currentLine)
                        }
                    }
                    !diffLine.startsWith("@") -> {
                        // Context line, move to next
                        currentLine++
                    }
                }
            }
            
            // Use the original line ending style when joining lines
            val finalContent = lines.joinToString(lineEnding)
            
            WriteCommandAction.runWriteCommandAction(project) {
                document.setText(finalContent)
            }
            
            return true
        } catch (e: Exception) {
            val message = "Error applying udiff to $filePath: ${e.message}"
            logger.error(message, e)
            showNotification(message, NotificationType.ERROR)
            return false
        }
    }

    /**
     * Finds or creates a file and returns its VirtualFile
     */
    private fun findOrCreateFile(filePath: String): VirtualFile? {
        val absolutePath = resolveFilePath(filePath)
        val file = File(absolutePath)
        
        // Create parent directories if they don't exist
        file.parentFile?.mkdirs()
        
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                val message = "Failed to create file $filePath: ${e.message}"
                logger.error(message, e)
                showNotification(message, NotificationType.ERROR)
                return null
            }
        }
        
        // Refresh VFS to see the new file
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile == null) {
            val message = "Failed to find or create file: $filePath"
            logger.error(message)
            showNotification(message, NotificationType.ERROR)
            return null
        }
        
        return virtualFile
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
    
    /**
     * Shows a notification in the IDE
     */
    private fun showNotification(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(project)
    }
}
