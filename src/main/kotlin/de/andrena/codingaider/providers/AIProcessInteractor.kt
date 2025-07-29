package de.andrena.codingaider.providers

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.CommandObserver

/**
 * Interface for interacting with AI provider processes
 */
interface AIProcessInteractor {
    val provider: AIProvider
    
    /**
     * Starts the AI provider process with the given command data
     * @param commandData The command data
     * @param observer Observer for command lifecycle events
     * @return true if the process started successfully, false otherwise
     */
    fun startProcess(commandData: CommandData, observer: CommandObserver): Boolean
    
    /**
     * Sends input to the running process
     * @param input The input string to send
     * @return true if input was sent successfully, false otherwise
     */
    fun sendInput(input: String): Boolean
    
    /**
     * Checks if the process is currently running
     * @return true if the process is running, false otherwise
     */
    fun isProcessRunning(): Boolean
    
    /**
     * Stops the running process
     * @return true if process was stopped successfully, false otherwise
     */
    fun stopProcess(): Boolean
    
    /**
     * Gets the current output from the process
     * @return The accumulated output as a string
     */
    fun getOutput(): String
    
    /**
     * Clears the accumulated output
     */
    fun clearOutput()
}