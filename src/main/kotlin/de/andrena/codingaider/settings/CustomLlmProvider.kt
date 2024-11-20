package de.andrena.codingaider.settings

data class CustomLlmProvider(
    val name: String,
    val type: ProviderType,
    val baseUrl: String? = null,
    val modelName: String,
    val displayName: String? = null
) {
    fun getEffectiveDisplayName(): String = displayName ?: "$name/$modelName"
}

enum class ProviderType(val requiresApiKey: Boolean, val requiresBaseUrl: Boolean) {
    OPENAI(true, true),
    OLLAMA(false, true),
    OPENROUTER(true, false);

    fun getApiKeyName(providerName: String): String = when(this) {
        OPENAI -> "${providerName}_API_KEY"
        OPENROUTER -> "OPENROUTER_API_KEY"
        OLLAMA -> "" // Ollama doesn't require an API key
    }

    fun getBaseUrlEnvName(providerName: String): String = when(this) {
        OPENAI -> "${providerName}_API_BASE"
        OLLAMA -> "OLLAMA_API_BASE"
        OPENROUTER -> "OPENROUTER_API_BASE"
    }
}
