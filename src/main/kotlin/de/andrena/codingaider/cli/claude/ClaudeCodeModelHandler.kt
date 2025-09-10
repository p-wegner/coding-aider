package de.andrena.codingaider.cli.claude

import de.andrena.codingaider.cli.BaseCliModelHandler
import de.andrena.codingaider.cli.ModelCapability

/**
 * Model handler for Claude Code CLI.
 * Handles model-specific operations and capabilities for Claude Code.
 */
class ClaudeCodeModelHandler : BaseCliModelHandler() {
    
    override val defaultModel: String get() = "claude-3-5-sonnet-20241022"
    
    override val availableModels: List<String> get() = listOf(
        "claude-3-5-sonnet-20241022",
        "claude-3-haiku-20240307",
        "claude-3-opus-20240229"
    )
    
    override val modelCapabilities: Map<String, Set<ModelCapability>> = mapOf(
        "claude-3-5-sonnet-20241022" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.CODE_REVIEW,
            ModelCapability.REFACTORING_ASSISTANCE,
            ModelCapability.TEST_GENERATION,
            ModelCapability.ARCHITECTURE_DESIGN,
            ModelCapability.PLANNING_AND_TASK_MANAGEMENT,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING,
            ModelCapability.STREAMING_RESPONSES,
            ModelCapability.PERFORMANCE_OPTIMIZATION
        ),
        "claude-3-haiku-20240307" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.CODE_REVIEW,
            ModelCapability.TEST_GENERATION,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.STREAMING_RESPONSES
        ),
        "claude-3-opus-20240229" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.CODE_REVIEW,
            ModelCapability.REFACTORING_ASSISTANCE,
            ModelCapability.TEST_GENERATION,
            ModelCapability.ARCHITECTURE_DESIGN,
            ModelCapability.PLANNING_AND_TASK_MANAGEMENT,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING,
            ModelCapability.STREAMING_RESPONSES,
            ModelCapability.PERFORMANCE_OPTIMIZATION
        )
    )
    
    override fun resolveModel(modelName: String): String {
        return when {
            modelName.startsWith("claude-") -> modelName
            else -> {
                // For Claude Code, we prefer the full model name
                when (modelName.lowercase()) {
                    "sonnet", "3.5-sonnet" -> "claude-3-5-sonnet-20241022"
                    "haiku", "3-haiku" -> "claude-3-haiku-20240307"
                    "opus", "3-opus" -> "claude-3-opus-20240229"
                    else -> defaultModel
                }
            }
        }
    }
    
    override fun getRequiredEnvironmentVariables(modelName: String): Map<String, String> {
        return mapOf(
            "ANTHROPIC_API_KEY" to (System.getenv("ANTHROPIC_API_KEY") ?: "")
        ).filterValues { it.isNotBlank() }
    }
    
        
    override fun validateModel(modelName: String): List<String> {
        val errors = super.validateModel(modelName).toMutableList()
        
        // Claude-specific validation
        val resolvedModel = resolveModel(modelName)
        if (resolvedModel !in availableModels) {
            errors.add("Model '$modelName' is not supported by Claude Code")
        }
        
        // Check if API key is set
        val apiKey = System.getenv("ANTHROPIC_API_KEY")
        if (apiKey.isNullOrBlank()) {
            errors.add("ANTHROPIC_API_KEY environment variable is required for Claude Code")
        }
        
        return errors
    }
    
    /**
     * Gets models that support long context processing.
     * @return List of models that support long context processing
     */
    fun getLongContextSupportedModels(): List<String> {
        return modelCapabilities.filter { (_, capabilities) ->
            capabilities.contains(ModelCapability.LONG_CONTEXT_PROCESSING)
        }.keys.toList()
    }
    
    /**
     * Gets models that support streaming responses.
     * @return List of models that support streaming responses
     */
    fun getStreamingSupportedModels(): List<String> {
        return modelCapabilities.filter { (_, capabilities) ->
            capabilities.contains(ModelCapability.STREAMING_RESPONSES)
        }.keys.toList()
    }
    
    /**
     * Gets models that support code generation.
     * @return List of models that support code generation
     */
    fun getCodeGenerationSupportedModels(): List<String> {
        return modelCapabilities.filter { (_, capabilities) ->
            capabilities.contains(ModelCapability.CODE_GENERATION)
        }.keys.toList()
    }
    
    /**
     * Gets the best model for a specific capability.
     * @param capability The capability to optimize for
     * @return The best model for the capability
     */
    fun getBestModelForCapability(capability: ModelCapability): String? {
        return when (capability) {
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.ARCHITECTURE_DESIGN -> "claude-3-5-sonnet-20241022"
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION -> "claude-3-opus-20240229"
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS -> "claude-3-5-sonnet-20241022"
            ModelCapability.PERFORMANCE_OPTIMIZATION -> "claude-3-haiku-20240307"
            else -> defaultModel
        }
    }
    
    /**
     * Gets model context window size.
     * @param modelName The model name
     * @return Context window size in tokens
     */
    fun getContextWindowSize(modelName: String): Int {
        return when (resolveModel(modelName)) {
            "claude-3-5-sonnet-20241022" -> 200000
            "claude-3-haiku-20240307" -> 200000
            "claude-3-opus-20240229" -> 200000
            else -> 100000 // Default fallback
        }
    }
    
    /**
     * Gets model pricing information (if available).
     * @param modelName The model name
     * @return Pricing information map
     */
    fun getModelPricing(modelName: String): Map<String, Double> {
        return when (resolveModel(modelName)) {
            "claude-3-5-sonnet-20241022" -> mapOf(
                "input" to 0.003,
                "output" to 0.015
            )
            "claude-3-haiku-20240307" -> mapOf(
                "input" to 0.00025,
                "output" to 0.00125
            )
            "claude-3-opus-20240229" -> mapOf(
                "input" to 0.015,
                "output" to 0.075
            )
            else -> emptyMap()
        }
    }
}