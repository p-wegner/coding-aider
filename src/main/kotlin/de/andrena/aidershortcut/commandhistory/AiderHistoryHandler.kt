package de.andrena.aidershortcut.commandhistory

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AiderHistoryHandler(projectPath: String) {
    private val historyFile = File(projectPath, ".aider.input.history")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun getHistory(): List<Pair<LocalDateTime, List<String>>> {
        if (!historyFile.exists()) return emptyList()

        return historyFile.readText()
            .split("\n# ")
            .drop(1)
            .map { entry ->
                val lines = entry.lines()
                val dateTime = LocalDateTime.parse(lines[0], dateTimeFormatter)
                val command = lines.drop(1).filter { it.startsWith("+") }.map { it.substring(1) }
                dateTime to command
            }
            .reversed()
    }
}


