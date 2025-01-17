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
                val command = lines.drop(1)
                    .run {
                        // For plan mode, we want to skip system prompts and only show user prompts
                        if (lines.any { it.contains("+<SystemPrompt>", false) }) {
                            filter { line -> 
                                !line.startsWith("+<SystemPrompt>") && 
                                !line.startsWith("+</SystemPrompt>") &&
                                !line.startsWith("+$STRUCTURED_MODE_MARKER")
                            }
                        } else if (lines.any { it.contains("+$STRUCTURED_MODE_MARKER", false) }) {
                            dropWhile { !it.startsWith("+$STRUCTURED_MODE_MARKER") }
                        } else {
                            this
                        }
                    }
                    .joinToString("\n") {
                        it.trim()
                            .removePrefix("+")
                            .removePrefix(STRUCTURED_MODE_MARKER)
                            .removePrefix("<UserPrompt>")
                            .removeSuffix("</UserPrompt>")
                            .trim()
                    }
                dateTime to command.split("\n").filter { it.isNotEmpty() }
            }
            .reversed()
    }

    fun getLastChatHistory(): String {
        if (!chatHistoryFile.exists()) return "No chat history available."

        return chatHistoryFile.readText()
            .split("\n#### ")
            .lastOrNull()?.trim() ?: "No chat history available."
    }

}
