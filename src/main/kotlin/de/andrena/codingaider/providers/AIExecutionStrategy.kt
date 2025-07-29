package de.andrena.codingaider.providers

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData

/**
 * Abstract interface for AI provider execution strategies
 */
interface AIExecutionStrategy {
    val provider: AIProvider
    val project: Project
    
    /**
     * Builds the command arguments for the specific AI provider
     * @param commandData The command data containing message, files, and options
     * @return List of command arguments
     */
    fun buildCommand(commandData: CommandData): MutableList<String>
    
    /**
     * Prepares the execution environment (environment variables, working directory, etc.)
     * @param processBuilder The process builder to configure
     * @param commandData The command data
     */
    fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData)
    
    /**
     * Performs any cleanup operations after command execution
     */
    fun cleanupAfterExecution()
    
    /**
     * Checks if the AI provider is available on the system
     * @return true if the provider is available, false otherwise
     */
    fun isProviderAvailable(): Boolean
    
    /**
     * Gets the display name for this execution strategy
     * @return A human-readable name for this strategy
     */
    fun getDisplayName(): String
}