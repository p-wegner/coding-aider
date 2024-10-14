package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.AiderPlanService.Companion.STRUCTURED_MODE_MARKER
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
                val command = lines.drop(1).dropWhile { !it.startsWith("+$STRUCTURED_MODE_MARKER") }
                    .joinToString("\n") {
                        it.trim()
                            .removePrefix("+")
                            .removePrefix(STRUCTURED_MODE_MARKER)
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

    companion object {
        @JvmStatic
        fun getInstance(project: Project): AiderHistoryService = project.service()
    }

}
