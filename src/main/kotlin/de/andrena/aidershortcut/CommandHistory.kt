package de.andrena.aidershortcut

import java.time.LocalDateTime

data class CommandHistory(
    val timestamp: LocalDateTime,
    val command: String
)