package de.andrena.aidershortcut.commandhistory

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommandHistoryManager(private val projectPath: String) {
    private val historyFile = File(projectPath, ".aider.input.history")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun loadHistory(): List<CommandHistory> {
        if (!historyFile.exists()) return emptyList()

        return historyFile.readText()
            .split("\n# ")
            .drop(1)
            .map { entry ->
                val lines = entry.lines()
                val dateTime = LocalDateTime.parse(lines[0], dateTimeFormatter)
                val command = lines.drop(1).joinToString("\n") { it.trim().removePrefix("+").trim() }
                CommandHistory(dateTime, command.split("\n").filter { it.isNotEmpty() })
            }
            .reversed()
    }
}
