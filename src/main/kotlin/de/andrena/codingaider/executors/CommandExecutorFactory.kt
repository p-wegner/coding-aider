package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.cli.CliFactory
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.GenericCliSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker

/**
 * Factory for creating the appropriate command executor based on the selected CLI tool.
 * This provides a unified interface for creating executors while maintaining backward compatibility.
 */
object CommandExecutorFactory {
    
    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(CommandExecutorFactory::class.java)
    
    /**
     * Creates a command executor for the specified command data.
     * @param commandData The command data to execute
     * @param project The project context
     * @param apiKeyChecker The API key checker to use
     * @return The appropriate command executor
     */
    fun createExecutor(
        commandData: CommandData,
        project: Project,
        apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
    ): CommandExecutorInterface {
        val genericSettings = GenericCliSettings.getInstance()
        val selectedCli = genericSettings.selectedCli
        
        return when (selectedCli.lowercase()) {
            "aider" -> {
                logger.info("Using legacy CommandExecutor for Aider")
                LegacyCommandExecutor(commandData, project, apiKeyChecker)
            }
            "claude", "claude-code" -> {
                logger.info("Using GenericCommandExecutor for Claude Code")
                GenericCommandExecutor(commandData, project, apiKeyChecker)
            }
            else -> {
                // Default to generic executor for any CLI
                logger.info("Using GenericCommandExecutor for CLI: $selectedCli")
                GenericCommandExecutor(commandData, project, apiKeyChecker)
            }
        }
    }
    
    /**
     * Creates a generic command executor for the selected CLI tool.
     * @param commandData The command data to execute
     * @param project The project context
     * @param apiKeyChecker The API key checker to use
     * @return A generic command executor
     */
    fun createGenericExecutor(
        commandData: CommandData,
        project: Project,
        apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
    ): GenericCommandExecutor {
        return GenericCommandExecutor(commandData, project, apiKeyChecker)
    }
    
    /**
     * Creates a legacy command executor for Aider.
     * @param commandData The command data to execute
     * @param project The project context
     * @param apiKeyChecker The API key checker to use
     * @return A legacy command executor
     */
    fun createLegacyExecutor(
        commandData: CommandData,
        project: Project,
        apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
    ): LegacyCommandExecutor {
        return LegacyCommandExecutor(commandData, project, apiKeyChecker)
    }
    
    /**
     * Gets the available CLI tools for execution.
     * @return List of available CLI tool names
     */
    fun getAvailableCliTools(): List<String> {
        return CliFactory.getSupportedCliTools()
    }
    
    /**
     * Validates that the selected CLI tool is available and properly configured.
     * @param cliName The CLI tool name to validate
     * @return List of validation errors, empty if valid
     */
    fun validateCliConfiguration(cliName: String): List<String> {
        return CliFactory.validateCliConfiguration(cliName)
    }
    
    /**
     * Checks if a specific CLI tool supports certain features.
     * @param cliName The CLI tool name
     * @param requiredFeatures Set of required features
     * @return true if all features are supported, false otherwise
     */
    fun supportsFeatures(cliName: String, requiredFeatures: Set<de.andrena.codingaider.cli.CliFeature>): Boolean {
        val cli = CliFactory.getCli(cliName) ?: return false
        return requiredFeatures.all { cli.supportsFeature(it) }
    }
}

/**
 * Interface for command executors.
 * This allows different executor implementations to be used interchangeably.
 */
interface CommandExecutorInterface {
    /**
     * Executes the command.
     * @return The command output
     */
    fun executeCommand(): String
    
    /**
     * Aborts the current command execution.
     */
    fun abort()
    
    /**
     * Gets information about the current CLI tool being used.
     * @return CLI tool information
     */
    fun getCliInfo(): String
}

/**
 * Legacy command executor wrapper for backward compatibility.
 * This wraps the original CommandExecutor to implement the new interface.
 */
class LegacyCommandExecutor(
    private val commandData: CommandData,
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker
) : CommandExecutorInterface {
    
    private val legacyExecutor: CommandExecutor
    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(LegacyCommandExecutor::class.java)
    
    init {
        legacyExecutor = CommandExecutor(commandData, project, apiKeyChecker)
    }
    
    override fun executeCommand(): String {
        return legacyExecutor.executeCommand()
    }
    
    override fun abort() {
        // Legacy executor doesn't have abort method, but we can try to interrupt
        logger.warn("Abort not supported by legacy executor")
    }
    
    override fun getCliInfo(): String {
        return "Using legacy Aider executor with ${commandData.files.size} files"
    }
}