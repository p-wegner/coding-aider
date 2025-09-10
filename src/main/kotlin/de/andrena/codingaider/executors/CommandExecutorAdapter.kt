package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.CommandSubject
import de.andrena.codingaider.settings.GenericCliSettings
import de.andrena.codingaider.utils.ApiKeyChecker

/**
 * Adapter class that provides backward compatibility with the old CommandExecutor interface.
 * This uses the new GenericCommandExecutor internally but maintains the same API.
 */
class CommandExecutorAdapter(
    private val commandData: CommandData,
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker
) : CommandSubject by GenericCommandSubject() {
    
    private val genericExecutor: GenericCommandExecutor
    
    init {
        // Check if we should use the new generic executor
        val useGenericExecutor = GenericCliSettings.getInstance().selectedCli != "aider"
        
        genericExecutor = if (useGenericExecutor) {
            GenericCommandExecutor(commandData, project, apiKeyChecker)
        } else {
            // Fallback to old behavior for Aider
            throw UnsupportedOperationException("Aider-specific execution should use CommandExecutor")
        }
    }
    
    /**
     * Executes the command using the appropriate executor.
     */
    fun executeCommand(): String {
        return genericExecutor.executeCommand()
    }
    
    /**
     * Gets the CLI interface being used by the generic executor.
     */
    fun getCliInterface() = genericExecutor.cliInterface
    
    /**
     * Gets information about the current CLI tool.
     */
    fun getCliInfo() = genericExecutor.getCliInfo()
}