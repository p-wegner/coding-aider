package de.andrena.codingaider.settings

enum class LlmProviderType(
    val displayName: String,
    val requiresApiKey: Boolean = true,
    val requiresBaseUrl: Boolean = false,
    val modelNamePrefix: String,
    val exampleModels: String
) {
    OPENAI("OpenAI", requiresApiKey = true, requiresBaseUrl = true, "openai", 
        "Examples: o1-preview, gpt-4o"),
    OLLAMA("Ollama", requiresApiKey = false, requiresBaseUrl = true, "ollama",
        "Examples: llama3:70b, qwen2.5-coder:32b"),
    OPENROUTER("OpenRouter", requiresApiKey = true, requiresBaseUrl = false, "openrouter",
        "Examples: anthropic/claude-3.5-sonnet, openai/gpt-4o, qwen/qwen-2.5-coder-32b-instruct"),
    VERTEX("Vertex AI", requiresApiKey = true, requiresBaseUrl = false, "vertex_ai",
        "Examples: claude-3-5-sonnet@20240620, gemini-pro@20240620");

    fun getApiKeyName(): String {
        return when (this) {
            OPENAI -> "OPENAI_API_KEY"
            OLLAMA -> "" // Ollama doesn't require an API key
            OPENROUTER -> "OPENROUTER_API_KEY"
        }
    }

}
