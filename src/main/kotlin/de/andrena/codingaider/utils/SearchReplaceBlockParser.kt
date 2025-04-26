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

@Service(Service.Level.PROJECT)
class SearchReplaceBlockParser(private val project: Project) {
    private val logger = Logger.getInstance(SearchReplaceBlockParser::class.java)
    private val modifiedFiles = mutableSetOf<String>()

    companion object {

        // Quadruple backtick format: filepath\n````lang?\nSEARCH\n...\nREPLACE\n````
        // Handles optional language, CRLF/LF, DOT_MATCHES_ALL for content, $ anchor.
        private val QUADRUPLE_REGEX = """(?m)^([^\r\n]+)\r?\n````([^\r\n]*)\r?\n<<<<<<< SEARCH\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n````$""".toRegex(RegexOption.DOT_MATCHES_ALL)

        // Triple backtick format with mandatory language: filepath\n```lang\nSEARCH\n...\nREPLACE\n```
        // Handles mandatory language ([^\r\n]+), CRLF/LF, DOT_MATCHES_ALL, $ anchor.
        private val LANGUAGE_TRIPLE_REGEX = """(?m)^([^\r\n]+)\r?\n```([^\r\n]+)\r?\n<<<<<<< SEARCH\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n```$""".toRegex(RegexOption.DOT_MATCHES_ALL)

        // Triple backtick format without language: filepath\n```\nSEARCH\n...\nREPLACE\n```
        private val SIMPLE_TRIPLE_REGEX = """(?m)^([^\r\n]+)\r?\n```\r?\n<<<<<<< SEARCH\r?\n(.*?)\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n```$""".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        // New file block format: filepath\n```lang?\n<<<<<<< SEARCH\n=======\ncontent\n>>>>>>> REPLACE\n```
        private val NEW_FILE_BLOCK_REGEX = """(?m)^([^\r\n]+)\r?\n```([^\r\n]*)\r?\n<<<<<<< SEARCH\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n```$""".toRegex(RegexOption.DOT_MATCHES_ALL)
        private val QUADRUPLE_NEW_FILE_REGEX = """(?m)^([^\r\n]+)\r?\n````([^\r\n]*)\r?\n<<<<<<< SEARCH\r?\n=======\r?\n(.*?)\r?\n>>>>>>> REPLACE\r?\n````$""".toRegex(RegexOption.DOT_MATCHES_ALL)



        // Pattern to identify the instruction prompt (remains the same)
        private val INSTRUCTION_PROMPT_PATTERN = Pattern.compile(
            """When making code changes, please format them as SEARCH/REPLACE blocks using this format:[\s\S]*?Make your changes precise and minimal\.""",
            Pattern.MULTILINE
        )

        private val DIFF_FENCED_REGEX = """```\n(.+?)\n<<<<<<< SEARCH\n([\s\S]*?)=======\n([\s\S]*?)>>>>>>> REPLACE\n```""".toRegex(RegexOption.MULTILINE)

        // Whole file replacement pattern - exclude matches containing SEARCH/REPLACE blocks
        private val WHOLE_REGEX = """(.+?)\n```(?:\w*)\n(?!.*?<<<<<<< SEARCH)([\s\S]*?)\n```""".toRegex(RegexOption.MULTILINE)

        // Unified diff format pattern - improved to avoid capturing descriptive text as filename
        private val UDIFF_REGEX = """```diff\n--- ([^\n]+?)\n\+\+\+ \1\n@@ .* @@\n([\s\S]*?)\n```""".toRegex(RegexOption.MULTILINE)
    }

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

    fun parseBlocks(text: String): List<EditBlock> {
        val blocks = mutableListOf<EditBlock>()
        
        // Filter out the instruction prompt if it exists in the text
        var remainingText = filterInstructionPrompt(text)
        val processedRanges = mutableListOf<IntRange>()

        // Function to check if a range overlaps with already processed ranges
        fun isProcessed(range: IntRange): Boolean {
            return processedRanges.any { it.first <= range.first && it.last >= range.last || range.first <= it.first && range.last >= it.last || range.first in it || range.last in it }
        }

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
            DIFF_FENCED_REGEX,         // ``` \n path \n <<< ... >>> \n ```
            WHOLE_REGEX,               // path \n ``` content ```
            UDIFF_REGEX                // ```diff ... ```
        )

        regexPatterns.forEach { regex ->
            val currentRegex: Regex = when (regex) {
                is Regex -> regex
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
    
    private fun filterInstructionPrompt(text: String): String {
        val instructionPrompt = de.andrena.codingaider.settings.AiderDefaults.PLUGIN_BASED_EDITS_INSTRUCTION.trim()
        
        var filteredText = text.replace(instructionPrompt, "")
        
        val matcher = INSTRUCTION_PROMPT_PATTERN.matcher(filteredText)
        if (matcher.find()) {
            filteredText = matcher.replaceAll("")
        }
        
        return filteredText
    }

    data class BlockResult(
        val block: EditBlock,
        val success: Boolean,
        val message: String? = null
    )

    fun getModifiedFiles(): List<String> {
        return modifiedFiles.toList()
    }

    fun clearModifiedFiles() {
        modifiedFiles.clear()
    }

    fun applyBlocks(blocks: List<EditBlock>): Map<String, Boolean> {
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
        
        return results.associate { it.block.filePath to it.success }
    }
    
    /**
     * Applies a search/replace block to a file
     */
    private fun applySearchReplaceBlock(block: EditBlock): Boolean {
        val absolutePath = resolveFilePath(block.filePath)
        val file = File(absolutePath)
        
        file.parentFile?.mkdirs()
        
        if (!file.exists()) {
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
            val content = file.readText()
            if (block.searchContent.isBlank()) {
                val message = "Attempted to apply SEARCH/REPLACE block with empty SEARCH section to existing file: ${block.filePath}. This operation is typically for creating new files. Skipping."
                logger.warn(message)
                return false // Indicate failure/skip
            } else {
                val normalizedContent = normalizeLineEndings(content)
                val normalizedSearchContent = normalizeLineEndings(block.searchContent)

                if (normalizedContent.contains(normalizedSearchContent)) {
                    val newContent = normalizedContent.replace(normalizedSearchContent, normalizeLineEndings(block.replaceContent))
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
    
    private fun normalizeLineEndings(text: String): String {
        return text.replace("\r\n", "\n")
    }

    private fun replaceWholeFile(filePath: String, newContent: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            val finalContent = if (file.exists()) {
                val existingContent = file.readText()
                if (existingContent.contains("\r\n")) {
                    // If the file uses CRLF, ensure the new content does too
                    normalizeLineEndings(newContent).replace("\n", "\r\n")
                } else {
                    normalizeLineEndings(newContent)
                }
            } else {
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

    private fun applyUdiffChange(filePath: String, diffContent: String): Boolean {
        val file = findOrCreateFile(filePath) ?: return false
        
        try {
            val document = FileDocumentManager.getInstance().getDocument(file)
                ?: return false
            
            val fileContent = document.text
            val lineEnding = if (fileContent.contains("\r\n")) "\r\n" else "\n"
            
            val normalizedContent = normalizeLineEndings(fileContent)
            val lines = normalizedContent.lines().toMutableList()
            
            var currentLine = 0
            val diffLines = normalizeLineEndings(diffContent).lines()
            
            for (diffLine in diffLines) {
                when {
                    diffLine.startsWith("+") && !diffLine.startsWith("+++") -> {
                        val lineContent = diffLine.substring(1)
                        lines.add(currentLine, lineContent)
                        currentLine++
                    }
                    diffLine.startsWith("-") && !diffLine.startsWith("---") -> {
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

    private fun findOrCreateFile(filePath: String): VirtualFile? {
        val absolutePath = resolveFilePath(filePath)
        val file = File(absolutePath)
        
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
        
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile == null) {
            val message = "Failed to find or create file: $filePath"
            logger.error(message)
            showNotification(message, NotificationType.ERROR)
            return null
        }
        
        return virtualFile
    }
    
    private fun resolveFilePath(filePath: String): String {
        val projectPath = project.basePath ?: ""
        return if (File(filePath).isAbsolute) {
            filePath
        } else {
            Paths.get(projectPath, filePath).normalize().toString()
        }
    }
    
    private fun refreshVirtualFile(filePath: String) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
        virtualFile?.refresh(false, false)
    }
    
    private fun showNotification(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(content, type)
            .notify(project)
    }
}
