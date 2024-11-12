package de.andrena.codingaider.executors.api

interface AiderProcessInteractor {
    /**
     * Sends a command to the Aider process and returns the response
     */
    fun sendCommand(command: String): String
    
    /**
     * Parses the output from Aider to detect specific patterns
     */
    fun parseOutput(output: String): AiderOutputState
    
    /**
     * Checks if the process is ready for the next command
     */
    fun isReadyForCommand(): Boolean
}

/**
 * Represents the current state of Aider's output
 */
data class AiderOutputState(
    val isPrompting: Boolean = false,
    val hasError: Boolean = false,
    val isWaitingForConfirmation: Boolean = false,
    val message: String = ""
)
