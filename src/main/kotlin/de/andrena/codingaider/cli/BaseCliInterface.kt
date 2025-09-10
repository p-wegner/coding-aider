package de.andrena.codingaider.cli

import de.andrena.codingaider.command.CommandData
import java.io.File

/**
 * Base implementation of CliInterface providing common functionality.
 * Specific CLI implementations can extend this class.
 */
abstract class BaseCliInterface : CliInterface {
    
    /**
     * Supported features by this CLI implementation.
     * Override in subclasses to specify supported features.
     */
    protected open val supportedFeatures: Set<CliFeature> = emptySet()
    
    /**
     * Argument mappings for this CLI implementation.
     * Override in subclasses to provide specific mappings.
     */
    open override val argumentMappings: Map<GenericArgument, CliArgument>
        get() = emptyMap()
    
    override fun supportsFeature(feature: CliFeature): Boolean {
        return feature in supportedFeatures
    }
    
            
    override fun validateCommandData(commandData: CommandData): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate required features
        if (commandData.files.isNotEmpty() && !supportsFeature(CliFeature.FILE_OPERATIONS)) {
            errors.add("This CLI tool does not support file operations")
        }
        
        if (commandData.files.any { it.isReadOnly } && !supportsFeature(CliFeature.READ_ONLY_FILES)) {
            errors.add("This CLI tool does not support read-only files")
        }
        
        if (commandData.useYesFlag && !supportsFeature(CliFeature.YES_FLAG)) {
            errors.add("This CLI tool does not support yes flag")
        }
        
        // Validate model
        if (commandData.llm.isNotBlank() && !getModelHandler().supportsModel(commandData.llm)) {
            errors.add("Model '${commandData.llm}' is not supported by this CLI tool")
        }
        
        // Validate files exist
        commandData.files.forEach { fileData ->
            val file = File(fileData.filePath)
            if (!file.exists()) {
                errors.add("File '${fileData.filePath}' does not exist")
            }
        }
        
        return errors
    }
    
    /**
     * Gets the model handler for this CLI implementation.
     * Subclasses should override this to provide a specific model handler.
     */
    override fun getModelHandler(): CliModelHandler {
        throw UnsupportedOperationException("Model handler not implemented for ${this::class.simpleName}")
    }
    
    /**
     * Gets the argument mapper for this CLI implementation.
     * Subclasses should override this to provide a specific argument mapper.
     */
    protected open fun getArgumentMapper(): CliArgumentMapper {
        throw UnsupportedOperationException("Argument mapper not implemented for ${this::class.simpleName}")
    }
    
    /**
     * Adds common environment variables to the process builder.
     */
    protected fun addCommonEnvironmentVariables(processBuilder: ProcessBuilder, commandData: CommandData) {
        val env = processBuilder.environment()
        
        // Add model-specific environment variables
        if (commandData.llm.isNotBlank()) {
            val modelEnvVars = getModelHandler().getRequiredEnvironmentVariables(commandData.llm)
            env.putAll(modelEnvVars)
        }
        
        // Add working directory if specified
        // Note: working directory handling is CLI-specific
        // Each CLI implementation should handle this as needed
    }
    
    /**
     * Validates that all required features are supported.
     * @param requiredFeatures Set of required features
     * @return List of unsupported features
     */
    protected fun validateRequiredFeatures(requiredFeatures: Set<CliFeature>): List<CliFeature> {
        return requiredFeatures.filter { !supportsFeature(it) }
    }
    
    /**
     * Logs a message for debugging purposes.
     * @param message The message to log
     */
    protected fun logDebug(message: String) {
        // TODO: Implement proper logging
        println("[DEBUG] ${this::class.simpleName}: $message")
    }
    
    /**
     * Logs an error message.
     * @param message The error message to log
     */
    protected fun logError(message: String) {
        // TODO: Implement proper logging
        System.err.println("[ERROR] ${this::class.simpleName}: $message")
    }
}