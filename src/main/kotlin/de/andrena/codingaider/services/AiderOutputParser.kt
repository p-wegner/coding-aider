package de.andrena.codingaider.services

import de.andrena.codingaider.executors.api.AiderOutputState

class AiderOutputParser {
    companion object {
        fun parseOutput(output: String): AiderOutputState {
            return when {
                output.contains("🤖>") -> AiderOutputState(isPrompting = true)
                output.contains("ERROR") -> AiderOutputState(hasError = true, message = output)
                output.contains("Confirm changes") -> AiderOutputState(isWaitingForConfirmation = true)
                else -> AiderOutputState()
            }
        }
    }
}
