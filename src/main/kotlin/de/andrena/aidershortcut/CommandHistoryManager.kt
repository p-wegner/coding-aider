package de.andrena.aidershortcut

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommandHistoryManager(private val projectPath: String) {
    private val historyFile = File(projectPath, ".aider.input.history")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun loadHistory(): List<CommandHistory> {
        if (!historyFile.exists()) return emptyList()

        return historyFile.readLines()
            .chunked(2)
            .mapNotNull { chunk ->
                if (chunk.size == 2 && chunk[0].startsWith("# ")) {
                    val dateTime = LocalDateTime.parse(chunk[0].substring(2), dateTimeFormatter)
                    val command = chunk[1].trim()
                    CommandHistory(dateTime, command)
                } else null
            }
            .reversed()
    }
}