package de.andrena.codingaider.settings

enum class LlmProviderType(
    val displayName: String,
    val requiresApiKey: Boolean = true,
    val requiresBaseUrl: Boolean = false,
    val modelNamePrefix: String
) {
    OPENAI("OpenAI", requiresApiKey = true, requiresBaseUrl = true, "openai"),
    OLLAMA("Ollama", requiresApiKey = false, requiresBaseUrl = true, "ollama"),
    OPENROUTER("OpenRouter", requiresApiKey = true, requiresBaseUrl = false, "openrouter");

    fun getApiKeyName(): String {
        return when (this) {
            OPENAI -> "OPENAI_API_KEY"
            OLLAMA -> "" // Ollama doesn't require an API key
            OPENROUTER -> "OPENROUTER_API_KEY"
        }
    }

}
