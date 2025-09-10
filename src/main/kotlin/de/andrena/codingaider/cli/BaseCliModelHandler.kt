package de.andrena.codingaider.cli

/**
 * Base implementation of CliModelHandler providing common functionality.
 * Specific CLI implementations can extend this class.
 */
abstract class BaseCliModelHandler : CliModelHandler {
    
    /**
     * Default model for this CLI implementation.
     */
    open override val defaultModel: String
        get() = "gpt-4"
    
    /**
     * List of available models for this CLI implementation.
     */
    open override val availableModels: List<String>
        get() = listOf("gpt-4", "gpt-3.5-turbo")
    
    /**
     * Model capability mappings.
     * Map of model names to their supported capabilities.
     */
    protected abstract val modelCapabilities: Map<String, Set<ModelCapability>>
    
            
    override fun supportsModel(modelName: String): Boolean {
        return modelName in availableModels || isCustomModel(modelName)
    }
    
    override fun supportsCapability(modelName: String, capability: ModelCapability): Boolean {
        return modelCapabilities[modelName]?.contains(capability) ?: false
    }
    
    /**
     * Checks if a model name represents a custom model.
     * @param modelName The model name to check
     * @return true if it's a custom model, false otherwise
     */
    protected open fun isCustomModel(modelName: String): Boolean {
        // Default implementation - can be overridden in subclasses
        return false
    }
    
    /**
     * Gets the base model name for a custom model.
     * @param modelName The potentially custom model name
     * @return The base model name
     */
    protected open fun getBaseModel(modelName: String): String {
        // Default implementation - can be overridden in subclasses
        return modelName
    }
    
    /**
     * Validates the model name.
     * @param modelName The model name to validate
     * @return List of validation errors, empty if valid
     */
    open fun validateModel(modelName: String): List<String> {
        val errors = mutableListOf<String>()
        
        if (modelName.isBlank()) {
            errors.add("Model name cannot be blank")
        }
        
        if (!supportsModel(modelName)) {
            errors.add("Model '$modelName' is not supported")
        }
        
        return errors
    }
    
    /**
     * Gets the model family for a given model.
     * @param modelName The model name
     * @return The model family (e.g., "gpt", "claude", "gemini")
     */
    protected open fun getModelFamily(modelName: String): String {
        return when {
            modelName.startsWith("gpt-") -> "gpt"
            modelName.startsWith("claude-") -> "claude"
            modelName.startsWith("gemini-") -> "gemini"
            modelName.startsWith("llama") -> "llama"
            modelName.startsWith("mistral") -> "mistral"
            else -> "unknown"
        }
    }
    
    /**
     * Gets the model provider for a given model.
     * @param modelName The model name
     * @return The model provider (e.g., "openai", "anthropic", "google")
     */
    protected open fun getModelProvider(modelName: String): String {
        return when (getModelFamily(modelName)) {
            "gpt" -> "openai"
            "claude" -> "anthropic"
            "gemini" -> "google"
            "llama" -> "meta"
            "mistral" -> "mistral"
            else -> "unknown"
        }
    }
    
    /**
     * Gets the model version for a given model.
     * @param modelName The model name
     * @return The model version
     */
    protected open fun getModelVersion(modelName: String): String {
        val parts = modelName.split("-")
        return if (parts.size >= 2) parts[1] else "unknown"
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