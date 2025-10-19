package de.andrena.codingaider.utils

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import de.andrena.codingaider.settings.*
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

interface ApiKeyChecker {
    fun isApiKeyAvailableForLlm(llm: String): Boolean
    fun isApiKeyAvailable(apiKeyName: String): Boolean
    fun getApiKeyForLlm(llm: String): String?
    fun getAllLlmOptions(): List<LlmSelection>
    fun getAllApiKeyNames(): List<String>
    fun getApiKeyValue(apiKeyName: String): String?
    fun getApiKeysForDocker(): Map<String, String>
    fun getLlmSelectionForName(webCrawlLlm: String): LlmSelection
}

@Service(Service.Level.APP)
class DefaultApiKeyChecker : ApiKeyChecker {
    private val llmToApiKeyMap = mapOf(
        "sonnet" to "ANTHROPIC_API_KEY",
        "haiku" to "ANTHROPIC_API_KEY",
        "openai/gpt-5" to "OPENAI_API_KEY",
        "gpt-4o-mini" to "OPENAI_API_KEY",
        "o4-mini" to "OPENAI_API_KEY",
        "deepseek" to "DEEPSEEK_API_KEY",
        "r1" to "DEEPSEEK_API_KEY",
        "gemini-2.5-pro" to "GEMINI_API_KEY",
        "gemini/gemini-2.5-flash-preview-05-20" to "GEMINI_API_KEY",
    )

    // API Key Cache
    private val apiKeyCache = ConcurrentHashMap<String, CachedApiKey>()
    private val CACHE_DURATION = Duration.ofMinutes(5)

    // Cached API Key data class
    private data class CachedApiKey(
        val value: String?,
        val timestamp: Instant = Instant.now()
    )

    fun clearCache() {
        apiKeyCache.clear()
    }

    fun clearCacheForKey(apiKeyName: String) {
        apiKeyCache.remove(apiKeyName)
    }

    private fun getCustomProvider(llm: String): CustomLlmProvider? {
        return service<CustomLlmProviderService>().getProvider(llm)
    }

    private fun getProviderApiKeyName(provider: CustomLlmProvider): List<String> {
        return when (provider.type) {
            LlmProviderType.OPENAI -> listOf("OPENAI_API_KEY")
            LlmProviderType.OPENROUTER -> listOf("OPENROUTER_API_KEY")
            LlmProviderType.OLLAMA -> emptyList() // Ollama doesn't require an API key
            LlmProviderType.VERTEX_EXPERIMENTAL -> listOf(
                "GOOGLE_APPLICATION_CREDENTIALS",
                "VERTEXAI_PROJECT",
                "VERTEXAI_LOCATION"
            )
            LlmProviderType.CUSTOM_AIDERMODEL -> emptyList()
            LlmProviderType.LMSTUDIO -> listOf("OPENAI_API_KEY", "LM_STUDIO_API_KEY") // LMStudio uses OpenAI-compatible endpoints
            LlmProviderType.GEMINI -> listOf("GEMINI_API_KEY")
        }
    }

    override fun isApiKeyAvailableForLlm(llm: String): Boolean {
        // Check standard providers first
        val standardApiKey = llmToApiKeyMap[llm]
        if (standardApiKey != null) {
            return isApiKeyAvailable(standardApiKey)
        }

        // Check custom providers
        val customProvider = getCustomProvider(llm)
        if (customProvider != null) {
            // Check if provider requires API key
            if (!customProvider.type.supportsApiKey) {
                return true
            }

            // For optional API keys, check both custom key and environment variables
            if (customProvider.type.apiKeyOptional) {
                // Check if custom API key is available
                val customKey = ApiKeyManager.getCustomModelKey(customProvider.name)
                if (customKey != null && customKey.isNotBlank()) {
                    return true
                }
                
                // Check environment variables as fallback
                return customProvider.type.getDefaultEnvironmentVariables().any { envVar ->
                    isEnvironmentVariableAvailable(envVar)
                }
            }

            // For required API keys, check for API key
            val apiKeyName = customProvider.type.getApiKeyName()
            return isApiKeyAvailable(apiKeyName)
        }

        return true // If no provider found, assume no API key needed
    }

    override fun isApiKeyAvailable(apiKeyName: String): Boolean {
        return getApiKeyValue(apiKeyName) != null
    }

    override fun getApiKeyForLlm(llm: String): String? {
        // Check standard providers first
        val standardApiKey = llmToApiKeyMap[llm]
        if (standardApiKey != null) {
            return getApiKeyValue(standardApiKey)
        }

        // Check custom providers
        val customProvider = getCustomProvider(llm)
        if (customProvider != null) {
            // For providers that don't require API keys
            if (!customProvider.type.supportsApiKey) {
                return null
            }

            // Check custom API key first
            val customKey = ApiKeyManager.getCustomModelKey(customProvider.name)
            if (customKey != null && customKey.isNotBlank()) {
                return customKey
            }

            // For optional API keys, fall back to environment variables
            if (customProvider.type.apiKeyOptional) {
                return customProvider.type.getDefaultEnvironmentVariables()
                    .firstNotNullOfOrNull { envVar -> getApiKeyValue(envVar) }
            }

            // For required API keys, return the API key name for environment lookup
            return getApiKeyValue(customProvider.type.getApiKeyName())
        }

        return null
    }

    override fun getAllLlmOptions(): List<LlmSelection> {
        val defaultSettings = service<DefaultProviderSettings>()
        val standardOptions = getStandardOptions(defaultSettings)
        val customOptions = service<CustomLlmProviderService>().getVisibleProviders()
            .filter { provider ->
                // Include providers that have authentication available or don't require it
                !provider.type.supportsApiKey || provider.hasAuthenticationAvailable()
            }
            .map { LlmSelection(it.name, provider = it, isBuiltIn = false) }
        return standardOptions + customOptions
    }

    private fun getStandardOptions(defaultSettings: DefaultProviderSettings = service<DefaultProviderSettings>()): List<LlmSelection> =
        listOf(LlmSelection("")) + llmToApiKeyMap.keys
            .filterNot { defaultSettings.isProviderHidden(it) }
            .map { LlmSelection(it, isBuiltIn = true) }

    override fun getAllApiKeyNames(): List<String> = llmToApiKeyMap.values.distinct()

    fun getAllStandardLlmKeys(): List<String> = llmToApiKeyMap.keys.toList()

    override fun getApiKeyValue(apiKeyName: String): String? {
        // Check cache first
        if (apiKeyName.isBlank()) return null
        apiKeyCache[apiKeyName]?.let { cachedKey ->
            if (Duration.between(cachedKey.timestamp, Instant.now()) < CACHE_DURATION) {
                return cachedKey.value
            }
        }

        // Retrieve API key
        val apiKey = retrieveApiKey(apiKeyName)

        // Cache the result (including null values to avoid repeated lookups)
        apiKeyCache[apiKeyName] = CachedApiKey(apiKey)

        return apiKey
    }

    private fun retrieveApiKey(apiKeyName: String): String? {
        // Check CredentialStore first
        ApiKeyManager.getApiKey(apiKeyName)?.let { return it }

        // Check environment variable
        System.getenv(apiKeyName)?.let { return it }

        // Check .env file in the user's home directory
        val homeEnvFile = File(System.getProperty("user.home"), ".env")
        readEnvFile(homeEnvFile)[apiKeyName]?.let { return it }

        // Check .env file in the current working directory
        val currentDirEnvFile = File(System.getProperty("user.dir"), ".env")
        readEnvFile(currentDirEnvFile)[apiKeyName]?.let { return it }

        return null
    }

    private fun readEnvFile(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return file.readLines()
            .filter { it.contains('=') }
            .associate { line ->
                val (key, value) = line.split('=', limit = 2)
                key.trim() to value.trim()
            }
    }

    private fun isEnvironmentVariableAvailable(envVarName: String): Boolean {
        return service<EnvironmentVariableManager>().isEnvironmentVariableAvailable(envVarName)
    }

    override fun getApiKeysForDocker(): Map<String, String> {
        val standardKeys = llmToApiKeyMap.values.distinct()
            .mapNotNull { apiKeyName ->
                getApiKeyValue(apiKeyName)?.let { apiKeyName to it }
            }

        val customKeys = service<CustomLlmProviderService>().getAllProviders()
            .filter { it.type.supportsApiKey }
            .flatMap { provider ->
                val keyNames = getProviderApiKeyName(provider)
                keyNames.mapNotNull { keyName ->
                    when (keyName) {
                        "GOOGLE_APPLICATION_CREDENTIALS" ->
                            System.getenv(keyName)?.let { keyName to it }

                        "VERTEXAI_PROJECT" ->
                            provider.projectId.takeIf { it.isNotEmpty() }?.let { keyName to it }

                        "VERTEXAI_LOCATION" ->
                            provider.location.takeIf { it.isNotEmpty() }?.let { keyName to it }

                        else -> null
                    }
                }
            }

        // Add environment variable fallbacks for custom providers
        val environmentFallbacks = service<CustomLlmProviderService>().getAllProviders()
            .filter { it.type.apiKeyOptional }
            .flatMap { provider ->
                provider.type.getDefaultEnvironmentVariables().mapNotNull { envVar ->
                    getApiKeyValue(envVar)?.let { envVar to it }
                }
            }

        return (standardKeys + customKeys + environmentFallbacks).toMap()
    }

    override fun getLlmSelectionForName(string: String): LlmSelection {
        return getStandardOptions().find { it.name == string }
            ?: service<CustomLlmProviderService>().getProvider(string).let {
                LlmSelection(string, provider = it, isBuiltIn = false)
            }


    }
}
