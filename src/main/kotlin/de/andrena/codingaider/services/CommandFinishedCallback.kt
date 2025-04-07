package de.andrena.codingaider.services

/**
 * Callback interface for command execution completion
 */
interface CommandFinishedCallback {
    /**
     * Called when a command has finished executing
     * @param exitCode The exit code of the command (0 for success)
     */
    fun onCommandFinished(exitCode: Int)
}
