package de.andrena.codingaider.cli.aider

import de.andrena.codingaider.cli.BaseCliModelHandler
import de.andrena.codingaider.cli.ModelCapability
import de.andrena.codingaider.settings.AiderSpecificSettings

/**
 * Model handler for Aider CLI.
 * Handles model-specific operations and capabilities for Aider.
 */
class AiderModelHandler : BaseCliModelHandler() {
    
    override val defaultModel: String get() = "gpt-4"
    
    override val availableModels: List<String> get() = listOf(
        "gpt-4",
        "gpt-4-turbo",
        "gpt-4o",
        "gpt-4o-mini",
        "claude-3-5-sonnet-20241022",
        "claude-3-haiku-20240307",
        "deepseek/deepseek-coder",
        "o1-mini",
        "o1-preview",
        "gemini/gemini-1.5-pro",
        "gemini/gemini-1.5-flash",
        "mistral/mistral-large-latest",
        "mistral/codestral-latest",
        "perplexity/llama-3.1-sonar-large-128k-online",
        "perplexity/llama-3.1-sonar-small-128k-online"
    )
    
    override val modelCapabilities: Map<String, Set<ModelCapability>> = mapOf(
        "gpt-4" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.GIT_INTEGRATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.CODE_REVIEW,
            ModelCapability.REFACTORING_ASSISTANCE,
            ModelCapability.TEST_GENERATION,
            ModelCapability.ARCHITECTURE_DESIGN,
            ModelCapability.PLANNING_AND_TASK_MANAGEMENT,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        ),
        "gpt-4-turbo" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.GIT_INTEGRATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.CODE_REVIEW,
            ModelCapability.REFACTORING_ASSISTANCE,
            ModelCapability.TEST_GENERATION,
            ModelCapability.ARCHITECTURE_DESIGN,
            ModelCapability.PLANNING_AND_TASK_MANAGEMENT,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        ),
        "gpt-4o" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.GIT_INTEGRATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.CODE_REVIEW,
            ModelCapability.REFACTORING_ASSISTANCE,
            ModelCapability.TEST_GENERATION,
            ModelCapability.ARCHITECTURE_DESIGN,
            ModelCapability.PLANNING_AND_TASK_MANAGEMENT,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        ),
        "gpt-4o-mini" to setOf(
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
            ModelCapability.CONTEXT_AWARENESS
        ),
        "claude-3-5-sonnet-20241022" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.GIT_INTEGRATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.CODE_REVIEW,
            ModelCapability.REFACTORING_ASSISTANCE,
            ModelCapability.TEST_GENERATION,
            ModelCapability.ARCHITECTURE_DESIGN,
            ModelCapability.PLANNING_AND_TASK_MANAGEMENT,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
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
            ModelCapability.CONTEXT_AWARENESS
        ),
        "deepseek/deepseek-coder" to setOf(
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
            ModelCapability.CONTEXT_AWARENESS
        ),
        "o1-mini" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS
        ),
        "o1-preview" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.REASONING_AND_PROBLEM_SOLVING,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        ),
        "gemini/gemini-1.5-pro" to setOf(
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
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        ),
        "gemini/gemini-1.5-flash" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        ),
        "mistral/mistral-large-latest" to setOf(
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
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS
        ),
        "mistral/codestral-latest" to setOf(
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
            ModelCapability.CONTEXT_AWARENESS
        ),
        "perplexity/llama-3.1-sonar-large-128k-online" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.WEB_SEARCH,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        ),
        "perplexity/llama-3.1-sonar-small-128k-online" to setOf(
            ModelCapability.TEXT_GENERATION,
            ModelCapability.CODE_GENERATION,
            ModelCapability.CODE_ANALYSIS,
            ModelCapability.NATURAL_LANGUAGE_UNDERSTANDING,
            ModelCapability.NATURAL_LANGUAGE_GENERATION,
            ModelCapability.WEB_SEARCH,
            ModelCapability.MULTI_FILE_PROCESSING,
            ModelCapability.FILE_EDITING,
            ModelCapability.FILE_CREATION,
            ModelCapability.DOCUMENTATION_GENERATION,
            ModelCapability.MULTI_TURN_CONVERSATION,
            ModelCapability.CONTEXT_AWARENESS,
            ModelCapability.LONG_CONTEXT_PROCESSING
        )
    )
    
    override fun resolveModel(modelName: String): String {
        return when {
            modelName.startsWith("--") -> modelName // Already prefixed for Aider
            modelName.contains("/") -> modelName // Already in provider/model format
            else -> {
                // Add appropriate prefix based on model family
                val family = getModelFamily(modelName)
                when (family) {
                    "gpt" -> modelName
                    "claude" -> "anthropic/$modelName"
                    "gemini" -> "google/$modelName"
                    "llama" -> "meta/$modelName"
                    "mistral" -> "mistral/$modelName"
                    "deepseek" -> "deepseek/$modelName"
                    else -> modelName
                }
            }
        }
    }
    
    override fun getRequiredEnvironmentVariables(modelName: String): Map<String, String> {
        val family = getModelFamily(modelName)
        val provider = getModelProvider(modelName)
        
        return when (provider) {
            "openai" -> mapOf<String, String>("OPENAI_API_KEY" to (System.getenv("OPENAI_API_KEY") ?: ""))
            "anthropic" -> mapOf<String, String>("ANTHROPIC_API_KEY" to (System.getenv("ANTHROPIC_API_KEY") ?: ""))
            "google" -> mapOf<String, String>("GOOGLE_API_KEY" to (System.getenv("GOOGLE_API_KEY") ?: ""))
            "meta" -> mapOf<String, String>()
            "mistral" -> mapOf<String, String>("MISTRAL_API_KEY" to (System.getenv("MISTRAL_API_KEY") ?: ""))
            "deepseek" -> mapOf<String, String>("DEEPSEEK_API_KEY" to (System.getenv("DEEPSEEK_API_KEY") ?: ""))
            else -> mapOf<String, String>()
        }.filterValues { it.isNotBlank() }
    }
    
        
    override fun validateModel(modelName: String): List<String> {
        val errors = super.validateModel(modelName).toMutableList()
        
        // Aider-specific validation
        if (!modelName.startsWith("--") && !modelName.contains("/")) {
            val family = getModelFamily(modelName)
            if (family == "unknown") {
                errors.add("Unknown model family for model: $modelName")
            }
        }
        
        // Check if required environment variables are set
        val requiredEnvVars = getRequiredEnvironmentVariables(modelName)
        requiredEnvVars.forEach { (key, value) ->
            if (value.isBlank()) {
                errors.add("Required environment variable '$key' is not set for model: $modelName")
            }
        }
        
        return errors
    }
    
    /**
     * Gets models that support reasoning effort.
     * @return List of models that support reasoning effort
     */
    fun getReasoningEffortSupportedModels(): List<String> {
        return modelCapabilities.filter { (_, capabilities) ->
            capabilities.contains(ModelCapability.REASONING_AND_PROBLEM_SOLVING)
        }.keys.toList()
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
     * Gets models that support web search.
     * @return List of models that support web search
     */
    fun getWebSearchSupportedModels(): List<String> {
        return modelCapabilities.filter { (_, capabilities) ->
            capabilities.contains(ModelCapability.WEB_SEARCH)
        }.keys.toList()
    }
    
    /**
     * Gets the best model for a specific capability.
     * @param capability The capability to optimize for
     * @return The best model for the capability
     */
    fun getBestModelForCapability(capability: ModelCapability): String? {
        return modelCapabilities.filter { (_, capabilities) ->
            capabilities.contains(capability)
        }.keys.firstOrNull() ?: defaultModel
    }
}