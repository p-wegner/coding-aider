package de.andrena.codingaider.cli

/**
 * Interface for handling model-specific operations for CLI tools.
 * Different AI coding assistants may have different model naming conventions
 * and requirements.
 */
interface CliModelHandler {
    /**
     * Resolves a generic model name to the CLI-specific model name.
     * @param modelName The generic model name (e.g., "gpt-4")
     * @return The CLI-specific model name
     */
    fun resolveModel(modelName: String): String
    
    /**
     * Gets the required environment variables for the specified model.
     * @param modelName The model name
     * @return Map of environment variable names to their values
     */
    fun getRequiredEnvironmentVariables(modelName: String): Map<String, String>
    
    /**
     * Checks if the model is supported by this CLI tool.
     * @param modelName The model name to check
     * @return true if the model is supported, false otherwise
     */
    fun supportsModel(modelName: String): Boolean
    
    /**
     * Gets the default model for this CLI tool.
     * @return The default model name
     */
    val defaultModel: String
    
    /**
     * Gets the list of available models for this CLI tool.
     * @return List of supported model names
     */
    val availableModels: List<String>
    
    /**
     * Checks if the model supports a specific capability.
     * @param modelName The model name
     * @param capability The capability to check
     * @return true if the model supports the capability, false otherwise
     */
    fun supportsCapability(modelName: String, capability: ModelCapability): Boolean
}