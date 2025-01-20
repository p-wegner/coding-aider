package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.plans.AiderPlanService.Companion.STRUCTURED_MODE_MARKER
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service(Service.Level.PROJECT)
class AiderHistoryService(private val project: Project) {
    private val inputHistoryFile = File(project.basePath, ".aider.input.history")
    private val chatHistoryFile = File(project.basePath, ".aider.chat.history.md")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun getInputHistory(): List<Pair<LocalDateTime, List<String>>> {
        if (!inputHistoryFile.exists()) return emptyList()

        return inputHistoryFile.readText()
            .split("\n# ")
            .drop(1)
            .map { entry ->
                val lines = entry.lines()
                val dateTime = LocalDateTime.parse(lines[0], dateTimeFormatter)
                val command = lines.drop(1).joinToString("\n") { it.trim() }
                    .let { fullText ->
                        if (fullText.contains("<SystemPrompt>")) {
                            fullText.substringAfterLast("<UserPrompt>")
                                .substringBefore("</UserPrompt>")
                                .trim()
                        } else if (fullText.contains(STRUCTURED_MODE_MARKER)) {
                            fullText.substringAfter(STRUCTURED_MODE_MARKER)
                                .substringBefore(STRUCTURED_MODE_MARKER)
                                .trim()
                        } else {
                            fullText
                        }
                    }
                    .split("\n")
                    .filter { it.isNotBlank() }
                    .joinToString("\n") { 
                        it.removePrefix("+")
                            .removePrefix(STRUCTURED_MODE_MARKER)
                            .removePrefix("<UserPrompt>")
                            .removeSuffix("</UserPrompt>")
                            .trim() 
                    }
                dateTime to command.split("\n").filter { it.isNotEmpty() }
            }
            .reversed()
    }

    private fun extractPromptContent(text: String): String {
        return text.lines()
            .filter { it.startsWith("####") }
            .map { it.removePrefix("####").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    private fun extractXmlPrompts(chatContent: String): Pair<String?, String?> {
        val systemPrompt = chatContent.substringBetween("<SystemPrompt>", "</SystemPrompt>")
            ?.let { extractPromptContent(it) }
        
        val userPrompt = chatContent.substringBetween("<UserPrompt>", "</UserPrompt>")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return Pair(systemPrompt, userPrompt)
    }

    private fun extractNonXmlPrompts(chatContent: String): Pair<String?, String?> {
        val promptLines = chatContent.lines()
            .takeWhile { !it.startsWith(">") }
            .filter { it.startsWith("####") }
            
        if (promptLines.isEmpty()) return Pair(null, null)
        
        val userPromptIndex = promptLines.indexOfFirst { it.contains("UserPrompt:") }
        
        return if (userPromptIndex != -1) {
            val systemPrompt = promptLines.take(userPromptIndex)
                .joinToString("\n") { it.removePrefix("####").trim() }
            val userPrompt = promptLines.drop(userPromptIndex + 1)
                .joinToString("\n") { it.removePrefix("####").trim() }
            Pair(systemPrompt, userPrompt)
        } else {
            Pair(extractPromptContent(promptLines.joinToString("\n")), null)
        }
    }

    fun getLastChatHistory(): String {
        if (!chatHistoryFile.exists()) return "No chat history available."

        val chatContent = chatHistoryFile.readText()
            .split("# aider chat started at .*".toRegex())
            .lastOrNull()?.trim() ?: return "No chat history available."

        // Try XML-tagged prompts first, fall back to non-XML format
        val (systemPrompt, userPrompt) = if (chatContent.contains("<SystemPrompt>")) {
            extractXmlPrompts(chatContent)
        } else {
            extractNonXmlPrompts(chatContent)
        }

        // Build cleaned output
        val promptSection = buildString {
            if (systemPrompt != null) {
                appendLine("System Prompt:")
                appendLine(systemPrompt)
                appendLine()
            }
            if (userPrompt != null) {
                appendLine("User Prompt:")
                appendLine(userPrompt)
                appendLine()
            }
        }

        // Get the chat content after prompts, handling both XML and non-XML formats
        val chatSection = if (chatContent.contains("</UserPrompt>")) {
            chatContent.substringAfter("</UserPrompt>")
        } else {
            chatContent.lines()
                .dropWhile { it.startsWith("####") || it.isBlank() }
                .joinToString("\n")
        }.trim()

        return promptSection + chatSection
    }

    private fun String.substringBetween(start: String, end: String): String? {
        val startIndex = this.indexOf(start)
        val endIndex = this.indexOf(end)
        
        if (startIndex != -1 && endIndex != -1) {
            return this.substring(startIndex + start.length, endIndex)
        }
        return null
    }

}
