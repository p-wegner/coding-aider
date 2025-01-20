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

    private fun cleanPromptSection(text: String): String {
        return text.lines()
            .filter { it.startsWith("####") }
            .map { it.removePrefix("####").trim() }
            .joinToString("\n")
    }

    private fun extractUserPrompt(text: String): String? {
        val userPromptStart = text.indexOf("<UserPrompt>")
        val userPromptEnd = text.indexOf("</UserPrompt>")
        
        if (userPromptStart != -1 && userPromptEnd != -1) {
            return text.substring(userPromptStart + 12, userPromptEnd).trim()
        }
        return null
    }

    fun getLastChatHistory(): String {
        if (!chatHistoryFile.exists()) return "No chat history available."

        val chatContent = chatHistoryFile.readText()
            .split("# aider chat started at .*".toRegex())
            .lastOrNull()?.trim() ?: return "No chat history available."

        // Extract system and user prompts
        val systemPromptSection = chatContent.substringBetween("<SystemPrompt>", "</SystemPrompt>")
            ?.let { cleanPromptSection(it) }
        val userPrompt = extractUserPrompt(chatContent)

        // Build cleaned output
        val promptSection = buildString {
            if (systemPromptSection != null) {
                appendLine("System Prompt:")
                appendLine(systemPromptSection)
                appendLine()
            }
            if (userPrompt != null) {
                appendLine("User Prompt:")
                appendLine(userPrompt)
                appendLine()
            }
        }

        // Get the rest of the chat content after the prompts
        val chatSection = chatContent.substringAfter("</UserPrompt>")
            .trim()
            .lines()
            .filter { !it.startsWith("####") }
            .joinToString("\n")

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
