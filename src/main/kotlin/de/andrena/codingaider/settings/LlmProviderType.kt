package de.andrena.codingaider.settings

enum class LlmProviderType(
    val displayName: String,
    val requiresApiKey: Boolean = true,
    val requiresBaseUrl: Boolean = false
) {
    OPENAI("OpenAI", requiresApiKey = true, requiresBaseUrl = true),
    OLLAMA("Ollama", requiresApiKey = false, requiresBaseUrl = true),
    OPENROUTER("OpenRouter", requiresApiKey = true, requiresBaseUrl = false);

    fun getApiKeyName(): String {
        return when (this) {
            OPENAI -> "OPENAI_API_KEY"
            OLLAMA -> "" // Ollama doesn't require an API key
            OPENROUTER -> "OPENROUTER_API_KEY"
        }
    }

    fun getBaseUrlName(): String {
        return when (this) {
            OPENAI -> "OPENAI_API_BASE"
            OLLAMA -> "OLLAMA_API_BASE"
            OPENROUTER -> "OPENROUTER_API_BASE"
        }
    }
}
