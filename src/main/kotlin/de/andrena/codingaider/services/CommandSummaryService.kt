package de.andrena.codingaider.services

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode

class CommandSummaryService {
    fun generateSummary(commandData: CommandData): String {
        return buildString {
            val message = commandData.message
            when (commandData.aiderMode) {
                AiderMode.ARCHITECT -> append("[ARCHITECT]")
                AiderMode.STRUCTURED -> append("[STRUCTURED]")
                AiderMode.SHELL -> append("[SHELL]")
                AiderMode.NORMAL -> append("[NORMAL]")
            }
            if (message.length > 20) {
                append(message.substring(0, 20))
                append("...")
            } else {
                append(message)
            }
        }
    }
}
