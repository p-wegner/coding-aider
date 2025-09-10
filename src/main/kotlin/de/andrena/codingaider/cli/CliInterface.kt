package de.andrena.codingaider.cli

import de.andrena.codingaider.command.CommandData
import java.io.File

/**
 * Core interface for CLI tool abstraction.
 * Defines the contract for different AI coding assistants (Aider, Claude Code, etc.).
 */
interface CliInterface {
    /**
     * Builds the command line arguments for the specific CLI tool.
     * @param commandData Generic command data containing prompt, files, and options
     * @return List of command line arguments ready for execution
     */
    fun buildCommand(commandData: CommandData): List<String>
    
    /**
     * Prepares the environment before command execution.
     * @param processBuilder The process builder to configure
     * @param commandData The command data for environment setup
     */
    fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData)
    
    /**
     * Performs cleanup after command execution.
     */
    fun cleanupAfterExecution()
    
    /**
     * Checks if the CLI tool supports a specific feature.
     * @param feature The feature to check support for
     * @return true if the feature is supported, false otherwise
     */
    fun supportsFeature(feature: CliFeature): Boolean
    
    /**
     * Gets the argument mappings for this CLI tool.
     * @return Map of generic arguments to CLI-specific arguments
     */
    val argumentMappings: Map<GenericArgument, CliArgument>
    
    /**
     * Gets the executable name for this CLI tool.
     * @return The executable name (e.g., "aider", "claude")
     */
    fun getExecutableName(): String
    
    /**
     * Gets the model handler for this CLI tool.
     * @return The model handler instance
     */
    fun getModelHandler(): CliModelHandler
    
    /**
     * Validates the command data for this CLI tool.
     * @param commandData The command data to validate
     * @return List of validation errors, empty if valid
     */
    fun validateCommandData(commandData: CommandData): List<String>
}