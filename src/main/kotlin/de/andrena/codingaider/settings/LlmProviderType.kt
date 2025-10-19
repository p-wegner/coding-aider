package de.andrena.codingaider.settings

enum class LlmProviderType(
    val displayName: String,
    val supportsApiKey: Boolean = true,
    val requiresBaseUrl: Boolean = false,
    val modelNamePrefix: String,
    val exampleModels: String,
    val authType: AuthType = AuthType.API_KEY,
    val requiresModelPrefix: Boolean = true,
    val apiKeyOptional: Boolean = false
) {
    OPENAI(
        "OpenAI", supportsApiKey = true, requiresBaseUrl = true, "openai",
        "Examples: o1-preview, gpt-4o", apiKeyOptional = true
    ),
    CUSTOM_AIDERMODEL(
        "Custom", false, false, "",
        "Examples: deepseek/deepseek-chat", AuthType.NONE, false
    ),
    OLLAMA(
        "Ollama", supportsApiKey = false, requiresBaseUrl = true, "ollama",
        "Examples: llama3:70b, qwen2.5-coder:32b",
        authType = AuthType.NONE
    ),
    OPENROUTER(
        "OpenRouter", supportsApiKey = true, requiresBaseUrl = false, "openrouter",
        "Examples: anthropic/claude-3.5-sonnet, openai/gpt-4o, qwen/qwen-2.5-coder-32b-instruct",
        apiKeyOptional = true
    ),
    VERTEX_EXPERIMENTAL(
        "Vertex AI",
        supportsApiKey = false,
        requiresBaseUrl = false,
        modelNamePrefix = "vertex_ai",
        exampleModels = "Examples: claude-3-sonnet@latest, gemini-pro@latest, claude-3-opus@latest",
        authType = AuthType.GCLOUD,
        requiresModelPrefix = false  // Vertex AI doesn't use model prefixes
    ),
    GEMINI(
        "Gemini",
        supportsApiKey = true,
        requiresBaseUrl = false,
        modelNamePrefix = "gemini",
        exampleModels = "Examples: gemini-2.5-pro, gemini-2.0-flash",
        authType = AuthType.API_KEY,
        requiresModelPrefix = false,
        apiKeyOptional = true
    ),
    LMSTUDIO(
        "LM Studio",
        supportsApiKey = true,
        requiresBaseUrl = true,
        modelNamePrefix = "lm_studio",
        exampleModels = "Examples: mistral-7b, llama2-13b",
        authType = AuthType.API_KEY,
        apiKeyOptional = true,
    );

    enum class AuthType {
        API_KEY,    // Standard API key authentication
        NONE,       // No authentication required
        GCLOUD,     // Uses Google Cloud authentication
    }

    fun getApiKeyName(): String {
        return when (authType) {
            AuthType.API_KEY -> when (this) {
                OPENAI -> "OPENAI_API_KEY"
                OPENROUTER -> "OPENROUTER_API_KEY"
                GEMINI -> "GEMINI_API_KEY"
                LMSTUDIO -> "LM_STUDIO_API_KEY"
                else -> ""
            }

            AuthType.NONE, AuthType.GCLOUD -> ""

        }
    }

    fun getDefaultEnvironmentVariables(): List<String> {
        return when (this) {
            OPENAI -> listOf("OPENAI_API_KEY")
            OPENROUTER -> listOf("OPENROUTER_API_KEY")
            GEMINI -> listOf("GEMINI_API_KEY")
            LMSTUDIO -> listOf("LM_STUDIO_API_KEY", "OPENAI_API_KEY")
            VERTEX_EXPERIMENTAL -> listOf("GOOGLE_APPLICATION_CREDENTIALS", "VERTEXAI_PROJECT", "VERTEXAI_LOCATION")
            OLLAMA -> listOf("OLLAMA_API_BASE")
            CUSTOM_AIDERMODEL -> emptyList()
        }
    }


}
