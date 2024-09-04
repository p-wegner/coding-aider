package de.andrena.aidershortcut.commandhistory

import java.time.LocalDateTime

data class CommandHistory(
    val timestamp: LocalDateTime,
    val command: String
)