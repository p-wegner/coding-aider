package de.andrena.aidershortcut.commandhistory

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AiderHistoryHandler(projectPath: String) {
    private val historyFile = File(projectPath, ".aider.input.history")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")

    fun getHistory(): List<Pair<LocalDateTime, String>> {
        if (!historyFile.exists()) return emptyList()

        return historyFile.readLines()
            .windowed(2, 1, partialWindows = true)
            .mapNotNull { lines ->
                if (lines.size == 2 && lines[0].startsWith("# ") && lines[1].startsWith("+")) {
                    val dateTime = LocalDateTime.parse(lines[0].substring(2).trim(), dateTimeFormatter)
                    val command = lines[1].substring(1).trim()
                    dateTime to command
                } else null
            }
            .reversed()
    }
}


