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

        return inputHistoryFile.readText().split("\n# ").drop(1).map { entry ->
                val lines = entry.lines()
                val dateTime = LocalDateTime.parse(lines[0], dateTimeFormatter)
                val command = lines.drop(1).joinToString("\n") { it.trim() }.let { fullText ->
                        if (fullText.contains("<SystemPrompt>")) {
                            fullText.substringAfterLast("<UserPrompt>").substringBefore("</UserPrompt>").trim()
                        } else if (fullText.contains(STRUCTURED_MODE_MARKER)) {
                            fullText.substringAfter(STRUCTURED_MODE_MARKER).substringBefore(STRUCTURED_MODE_MARKER)
                                .trim()
                        } else {
                            fullText
                        }
                    }.split("\n").filter { it.isNotBlank() }.joinToString("\n") {
                        it.removePrefix("+").removePrefix(STRUCTURED_MODE_MARKER).removePrefix("<UserPrompt>")
                            .removeSuffix("</UserPrompt>").trim()
                    }
                dateTime to command.split("\n").filter { it.isNotEmpty() }
            }.reversed()
    }

    private fun extractXmlPrompts(chatContent: String): Triple<String?, String?, String?> {
        val systemPromptRegex = "(?s)<SystemPrompt>\\s*(.*?)\\s*</SystemPrompt>".toRegex()
        val userPromptRegex = "(?s)<UserPrompt>\\s*(.*?)\\s*</UserPrompt>".toRegex()

        val systemPrompt = systemPromptRegex.find(chatContent)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

        val userPrompt = userPromptRegex.find(chatContent)?.groupValues?.get(1)?.trim()

        // Extract everything after </UserPrompt> until the next chat session or end
        val outputStart = chatContent.indexOf("</UserPrompt>")
        val outputEnd =
            chatContent.indexOf("# aider chat started at", outputStart + 1).takeIf { it >= 0 } ?: chatContent.length

        val aiderOutput = if (outputStart >= 0) {
            chatContent.substring(outputStart + 12, outputEnd).trim().takeIf { it.isNotEmpty() }
        } else null

        return Triple(systemPrompt, userPrompt, aiderOutput)
    }

    fun getLastChatHistory(): String {
        if (!chatHistoryFile.exists()) return "No chat history available."

        val chatContent = chatHistoryFile.readText(Charsets.UTF_8)
        val contentParts = chatContent.split("# aider chat started at .*".toRegex())
        val lastChat = contentParts.lastOrNull()?.trim() ?: return "No chat history available."
        if (!containsXmlPrompts(lastChat)) {
            return lastChat
        }
        // Extract prompts and output from XML blocks
        val (systemPrompt, userPrompt, aiderOutput) = extractXmlPrompts(lastChat)

        return buildString {
            systemPrompt?.let { prompt ->
                appendLine("## **System Prompt**  \n")
                appendLine("```plaintext\n${prompt.replace("####", "").trim()}\n```")
                appendLine("\n---")
            }

            userPrompt?.let { prompt ->
                appendLine("## User Request\n")
                appendLine("```plaintext\n${prompt.replace("####", "").trim()}\n```")
                appendLine("\n---")
            }

            appendLine("## Aider Execution\n")
            appendLine("${aiderOutput ?: "<!-- No execution output captured -->"}\n")
        }.trim()
    }


    private fun containsXmlPrompts(aiderOutput: String): Boolean {
        return aiderOutput.contains("<SystemPrompt>") || aiderOutput.contains("<UserPrompt>")
    }
}

