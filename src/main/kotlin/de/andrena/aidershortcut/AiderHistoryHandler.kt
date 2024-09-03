package de.andrena.aidershortcut

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AiderHistoryHandler(private val projectPath: String) {
    private val historyFile = File(projectPath, ".aider.input.history")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun getHistory(): List<Pair<LocalDateTime, String>> {
        if (!historyFile.exists()) return emptyList()

        return historyFile.readLines()
            .chunked(2)
            .mapNotNull { chunk ->
                if (chunk.size == 2 && chunk[0].startsWith("# ")) {
                    val dateTime = LocalDateTime.parse(chunk[0].substring(2), dateTimeFormatter)
                    val command = chunk[1].trim()
                    dateTime to command
                } else null
            }
            .reversed()
    }

    fun addToHistory(command: String) {
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        historyFile.appendText("# $timestamp\n$command\n")
    }
}