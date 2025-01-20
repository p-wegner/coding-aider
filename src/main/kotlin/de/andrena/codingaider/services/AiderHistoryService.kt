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

    private fun extractXmlPrompts(chatContent: String): Triple<String?, String?, String?> {
        val systemPrompt = chatContent.substringBetween("<SystemPrompt>([\\s\\S]*?)</SystemPrompt>".toRegex())
            ?.replace("<SystemPrompt>|</SystemPrompt>".toRegex(), "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        
        val userPrompt = chatContent.substringBetween("<UserPrompt>([\\s\\S]*?)</UserPrompt>".toRegex())
            ?.replace("<UserPrompt>|</UserPrompt>".toRegex(), "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val aiderOutput = chatContent.substringAfterLast("</UserPrompt>")
            ?.substringBefore("# aider chat started at")
            ?.trim()

        return Triple(systemPrompt, userPrompt, aiderOutput)
    }

    fun getLastChatHistory(): String {
        if (!chatHistoryFile.exists()) return "No chat history available."

        val chatContent = chatHistoryFile.readText()
        val contentParts = chatContent.split("# aider chat started at .*".toRegex())
        val lastChat = contentParts.lastOrNull()?.trim() ?: return "No chat history available."

        // Extract prompts and output from XML blocks
        val (systemPrompt, userPrompt, aiderOutput) = extractXmlPrompts(lastChat)
        
        return buildString {
            systemPrompt?.let { prompt ->
                appendLine("System Context:")
                appendLine(prompt.replace("####", "").trim())
                appendLine()
            }
            
            userPrompt?.let { prompt ->
                appendLine("User Request:")
                appendLine(prompt.replace("####", "").trim())
                appendLine()
            }
            
            appendLine("Aider Output:")
            appendLine(aiderOutput ?: "No output captured")
        }.trim()
    }

    private fun String.substringBetween(regex: Regex): String? {
        val match = regex.find(this)
        return match?.groups?.get(1)?.value?.trim()
    }

}
