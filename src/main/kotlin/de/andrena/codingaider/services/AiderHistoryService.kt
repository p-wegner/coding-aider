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


    fun getLastChatHistory(): String {
        if (!chatHistoryFile.exists()) return "No chat history available."

        val chatContent = chatHistoryFile.readText()
        val contentParts = chatContent.split("# aider chat started at .*".toRegex())
        val lastChat = contentParts.lastOrNull()?.trim() ?: return "No chat history available."

        // Extract prompts from XML blocks
        val (systemPrompt, userPrompt) = extractXmlPrompts(lastChat)
        
        // Build cleaned output without duplicate prompts
        return buildString {
            // Add system prompt if found
            systemPrompt?.let { prompt ->
                appendLine("System Prompt:")
                appendLine(prompt.trim().replace("<SystemPrompt>|</SystemPrompt>".toRegex(), ""))
                appendLine()
            }
            
            // Add user prompt if found
            userPrompt?.let { prompt ->
                appendLine("User Prompt:")
                appendLine(prompt.trim().replace("<UserPrompt>|</UserPrompt>".toRegex(), ""))
                appendLine()
            }
            
            // Add cleaned chat content
            var inPromptSection = false
            lastChat.lines().forEach { line ->
                when {
                    line.startsWith("<SystemPrompt>") -> inPromptSection = true
                    line.startsWith("</SystemPrompt>") -> inPromptSection = false
                    line.startsWith("<UserPrompt>") -> inPromptSection = true
                    line.startsWith("</UserPrompt>") -> inPromptSection = false
                    line.startsWith("####") -> inPromptSection = true
                    !inPromptSection && line.isNotBlank() -> appendLine(line)
                }
            }
        }.trim()
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
