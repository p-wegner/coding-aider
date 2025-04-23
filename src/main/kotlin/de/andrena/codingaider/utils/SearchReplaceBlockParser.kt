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
        // Standard search/replace block pattern
        private val STANDARD_REGEX = """(?m)^([^\n]+)\n```+([^\n]*)\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```+""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // Quadruple backtick format
        private val QUADRUPLE_REGEX = """(?m)^([^\n]+)\n````([^\n]*)\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n````""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // Additional pattern for code blocks with language specified (like in prompt.txt)
        private val LANGUAGE_CODE_BLOCK_REGEX = """(?m)^([^\n]+)\n```([^\n]+)\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```+""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // Pattern for prompt.txt format with nested code blocks
        private val PROMPT_TXT_REGEX = """(?m)^([^\n]+)\n```+([^\n]*)\n```+([^\n]*)\n<<<<<<< SEARCH\n(.*?)\n=======\n(.*?)\n>>>>>>> REPLACE\n```+""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // Pattern to identify the instruction prompt
        private val INSTRUCTION_PROMPT_PATTERN = Pattern.compile(
            """When making code changes, please format them as SEARCH/REPLACE blocks using this format:[\s\S]*?Make your changes precise and minimal\.""",
            Pattern.MULTILINE
        )
        
        // Alternative format with fenced blocks (diff-fenced)
        private val DIFF_FENCED_PATTERN = Pattern.compile(
            """```\n(.+?)\n<<<<<<< SEARCH\n([\s\S]*?)=======\n([\s\S]*?)>>>>>>> REPLACE\n```""",
            Pattern.MULTILINE
        )
        
        // Whole file replacement pattern - exclude matches containing SEARCH/REPLACE blocks
        private val WHOLE_PATTERN = Pattern.compile(
            """(.+?)\n```(?:\w*)\n(?!.*?<<<<<<< SEARCH)([\s\S]*?)\n```""",
            Pattern.MULTILINE
        )
        
        // Unified diff format pattern
        private val UDIFF_PATTERN = Pattern.compile(
            """```diff\n--- (.+?)\n\+\+\+ \1\n@@ .* @@\n([\s\S]*?)\n```""",
            Pattern.MULTILINE
        )
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

        // Define the order of regex patterns to try (most specific first)
        val regexPatterns = listOf(
            QUADRUPLE_REGEX, // ````` ... `````
            LANGUAGE_CODE_BLOCK_REGEX, // ```lang ... ```+
            PROMPT_TXT_REGEX, // ```lang ``` ... ```+ (Nested - less likely for prompt.txt example)
            STANDARD_REGEX, // ```+ ... ```+ (More general)
            DIFF_FENCED_PATTERN, // ``` \n path \n <<< ... >>> \n ```
            WHOLE_PATTERN, // path \n ``` content ```
            UDIFF_PATTERN // ```diff ... ```
        )

        // Iterate through patterns and find matches
        regexPatterns.forEach { regex ->
            regex.findAll(remainingText).forEach { match ->
                if (!isProcessed(match.range)) {
                    try {
                        when (regex) {
                            STANDARD_REGEX -> {
                                val (filePath, language, searchContent, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = language.trim(),
                                    searchContent = searchContent,
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            QUADRUPLE_REGEX -> {
                                val (filePath, language, searchContent, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = language.trim(),
                                    searchContent = searchContent,
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            LANGUAGE_CODE_BLOCK_REGEX -> {
                                val (filePath, language, searchContent, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = language.trim(),
                                    searchContent = searchContent,
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            PROMPT_TXT_REGEX -> {
                                val (filePath, outerLanguage, innerLanguage, searchContent, replaceContent) = match.destructured
                                blocks.add(EditBlock(
                                    filePath = filePath.trim(),
                                    language = innerLanguage.trim().ifEmpty { outerLanguage.trim() },
                                    searchContent = searchContent,
                                    replaceContent = replaceContent,
                                    editType = EditType.SEARCH_REPLACE
                                ))
                                addProcessedRange(match.range)
                            }
                            // Handling for Pattern-based regexes (DIFF_FENCED, WHOLE, UDIFF)
                            // Note: These need separate handling as they don't use destructuring
                            // We'll handle them after the Regex-based ones.
                        }
                    } catch (e: Exception) {
                        logger.warn("Error processing match for regex $regex: ${e.message}", e)
                    }
                }
            }
        }


        // Process diff-fenced format (Pattern based)
        val diffFencedMatcher = DIFF_FENCED_PATTERN.matcher(remainingText)
        while (diffFencedMatcher.find()) {
            val range = diffFencedMatcher.start()..diffFencedMatcher.end()
            if (!isProcessed(range)) {
                blocks.add(
                    EditBlock(
                        filePath = diffFencedMatcher.group(1).trim(),
                        searchContent = diffFencedMatcher.group(2),
                        replaceContent = diffFencedMatcher.group(3),
                        editType = EditType.SEARCH_REPLACE // Diff-fenced is a type of Search/Replace
                    )
                )
                addProcessedRange(range)
            }
        }

        // Process whole file replacements (Pattern based)
        val wholeMatcher = WHOLE_PATTERN.matcher(remainingText)
        while (wholeMatcher.find()) {
            val range = wholeMatcher.start()..wholeMatcher.end()
            // Avoid matching blocks already identified as SEARCH_REPLACE or UDIFF
            val content = wholeMatcher.group(0)
            if (!isProcessed(range) && !content.contains("<<<<<<< SEARCH") && !content.startsWith("```diff")) {
                 blocks.add(
                    EditBlock(
                        filePath = wholeMatcher.group(1).trim(),
                        replaceContent = wholeMatcher.group(2),
                        editType = EditType.WHOLE_FILE
                    )
                )
                addProcessedRange(range)
            }
        }

        // Process unified diff format (Pattern based)
        val udiffMatcher = UDIFF_PATTERN.matcher(remainingText)
        while (udiffMatcher.find()) {
             val range = udiffMatcher.start()..udiffMatcher.end()
             if (!isProcessed(range)) {
                blocks.add(
                    EditBlock(
                        filePath = udiffMatcher.group(1).trim(),
                        replaceContent = udiffMatcher.group(2), // The actual diff content
                        editType = EditType.UDIFF
                    )
                )
                addProcessedRange(range)
             }
        }


        // --- Remove the old sequential processing logic ---
        /*
        // Process standard search/replace format
        STANDARD_REGEX.findAll(filteredText).forEach { match ->
            val (filePath, language, searchContent, replaceContent) = match.destructured
            blocks.add(
                EditBlock(
                    filePath = filePath.trim(),
                    language = language.trim(),
                    searchContent = searchContent,
                    replaceContent = replaceContent,
                    editType = EditType.SEARCH_REPLACE
                )
            )
        }
        
        // Process quadruple backtick format
        QUADRUPLE_REGEX.findAll(filteredText).forEach { match ->
            val (filePath, language, searchContent, replaceContent) = match.destructured
            blocks.add(
                EditBlock(
                    filePath = filePath.trim(),
                    language = language.trim(),
                    searchContent = searchContent,
                    replaceContent = replaceContent,
                    editType = EditType.SEARCH_REPLACE
                )
            )
        }
        
        // Process language-specific code block format (like in prompt.txt)
        LANGUAGE_CODE_BLOCK_REGEX.findAll(filteredText).forEach { match ->
            val (filePath, language, searchContent, replaceContent) = match.destructured
            blocks.add(
                EditBlock(
                    filePath = filePath.trim(),
                    language = language.trim(),
                    searchContent = searchContent,
                    replaceContent = replaceContent,
                    editType = EditType.SEARCH_REPLACE
                )
            )
        }
        
        // Process prompt.txt format with nested code blocks
        PROMPT_TXT_REGEX.findAll(filteredText).forEach { match ->
            val (filePath, outerLanguage, innerLanguage, searchContent, replaceContent) = match.destructured
            blocks.add(
                EditBlock(
                    filePath = filePath.trim(),
                    language = innerLanguage.trim().ifEmpty { outerLanguage.trim() },
                    searchContent = searchContent,
                    replaceContent = replaceContent,
                    editType = EditType.SEARCH_REPLACE
                )
            )
        }
        
        // Process diff-fenced format
        val diffFencedMatcher = DIFF_FENCED_PATTERN.matcher(filteredText)
        while (diffFencedMatcher.find()) {
            blocks.add(
                EditBlock(
                    filePath = diffFencedMatcher.group(1).trim(),
                    searchContent = diffFencedMatcher.group(2),
                    replaceContent = diffFencedMatcher.group(3),
                    editType = EditType.SEARCH_REPLACE
                )
            )
        }
        
        // Process whole file replacements
        val wholeMatcher = WHOLE_PATTERN.matcher(filteredText)
        while (wholeMatcher.find()) {
            blocks.add(
                EditBlock(
                    filePath = wholeMatcher.group(1).trim(),
                    replaceContent = wholeMatcher.group(2),
                    editType = EditType.WHOLE_FILE
                )
            )
        }
        
        // Process unified diff format
        val udiffMatcher = UDIFF_PATTERN.matcher(filteredText)
        while (udiffMatcher.find()) {
            blocks.add(
                EditBlock(
                    filePath = udiffMatcher.group(1).trim(),
                    replaceContent = udiffMatcher.group(2),
                    editType = EditType.UDIFF
                )
            )
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
            // For new files, we'll create them regardless of search content
            // This handles both empty search blocks and cases where the search block exists but is effectively empty
            file.writeText(block.replaceContent)
            refreshVirtualFile(absolutePath)
            return true
        } else {
            // Modifying an existing file
            val content = file.readText()
            if (block.searchContent.isBlank()) {
                // Empty search content means append to file
                file.writeText(content + block.replaceContent)
                refreshVirtualFile(absolutePath)
                return true
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
