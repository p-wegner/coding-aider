package de.andrena.codingaider.history

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AiderHistoryHandler(private val projectPath: String) {
    private val inputHistoryFile = File(projectPath, ".aider.input.history")
    private val chatHistoryFile = File(projectPath, ".aider.chat.history.md")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun getInputHistory(): List<Pair<LocalDateTime, List<String>>> {
        if (!inputHistoryFile.exists()) return emptyList()

        return inputHistoryFile.readText()
            .split("\n# ")
            .drop(1)
            .map { entry ->
                val lines = entry.lines()
                val dateTime = LocalDateTime.parse(lines[0], dateTimeFormatter)
                val command = lines.drop(1).joinToString("\n") { it.trim().removePrefix("+").trim() }
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