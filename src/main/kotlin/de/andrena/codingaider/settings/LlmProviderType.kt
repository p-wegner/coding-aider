package de.andrena.codingaider.settings

enum class LlmProviderType(
    val displayName: String,
    val requiresApiKey: Boolean = true,
    val requiresBaseUrl: Boolean = false,
    val modelNamePrefix: String,
    val exampleModels: String
) {
    OPENAI("OpenAI", requiresApiKey = true, requiresBaseUrl = true, "openai", 
        "Examples: gpt-4-turbo-preview, gpt-3.5-turbo"),
    OLLAMA("Ollama", requiresApiKey = false, requiresBaseUrl = true, "ollama",
        "Examples: llama2, codellama, mistral"),
    OPENROUTER("OpenRouter", requiresApiKey = true, requiresBaseUrl = false, "openrouter",
        "Examples: anthropic/claude-3-opus, google/gemini-pro, meta-llama/llama-2-70b");

    fun getApiKeyName(): String {
        return when (this) {
            OPENAI -> "OPENAI_API_KEY"
            OLLAMA -> "" // Ollama doesn't require an API key
            OPENROUTER -> "OPENROUTER_API_KEY"
        }
    }

}
