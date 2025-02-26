package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import de.andrena.codingaider.utils.GitUtils
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.plans.AiderPlanService.Companion.STRUCTURED_MODE_MARKER
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service(Service.Level.PROJECT)
class AiderHistoryService(private val project: Project) {
    private val gitRoot = GitUtils.findGitRoot(File(project.basePath!!))
    private val inputHistoryFile = File(gitRoot?.path ?: project.basePath, ".aider.input.history")
    private val chatHistoryFile = File(gitRoot?.path ?: project.basePath, ".aider.chat.history.md")
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
        
        // Extract prompts and output from XML blocks
        val (systemPrompt, userPrompt, aiderOutput) = extractXmlPrompts(lastChat)
        
        // Filter out Aider's echo of the prompts from the output
        val filteredOutput = aiderOutput?.let { output ->
            val cleanOutput = output.trim()
                
            // Extract and keep initialization info
            val initInfo = Regex("""Aider v[\d.]+ [^\n]*(?:\n(?:[^\n]+))*(?:\nAdded [^\n]+)*""")
                .find(cleanOutput)?.value

            // Remove Aider's echo of prompts (marked with #### prefix)
            var filtered = cleanOutput.replace(
                Regex("""(?m)^#### <(?:System|User)Prompt>[\s\S]*?#### </(?:System|User)Prompt>\s*"""),
                ""
            )

            // Remove any remaining lines starting with ####
            filtered = filtered.lines()
                .filterNot { it.trimStart().startsWith("####") }
                .joinToString("\n")

            // Remove redundant prompt displays that might appear in the output
            systemPrompt?.let { sysPrompt ->
                filtered = filtered.replace(
                    Regex("""(?s)## \*\*System Prompt\*\*\s*```plaintext\s*${Regex.escape(sysPrompt.trim())}\s*```\s*---\s*"""),
                    ""
                )
            }
                
            userPrompt?.let { usrPrompt ->
                filtered = filtered.replace(
                    Regex("""(?s)## User Request\s*```plaintext\s*${Regex.escape(usrPrompt.trim())}\s*```\s*---\s*"""),
                    ""
                )
            }

            // Keep only the first occurrence of init info if it exists
            val finalOutput = if (initInfo != null) {
                filtered.replace(Regex(Regex.escape(initInfo)), "").trim()
                    .let { if (it.isNotEmpty()) "$initInfo\n\n$it" else initInfo }
            } else {
                filtered
            }
                
            finalOutput.trim()
        }

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

            // Don't include "## Aider Execution" header when there's no output
            if (filteredOutput != null) {
                appendLine("## Aider Execution\n")
                appendLine("$filteredOutput\n")
            } else {
                appendLine("<!-- No execution output captured -->")
            }
        }.trim()
    }


    private fun containsXmlPrompts(aiderOutput: String): Boolean {
        return aiderOutput.contains("<SystemPrompt>") || aiderOutput.contains("<UserPrompt>")
    }
}

