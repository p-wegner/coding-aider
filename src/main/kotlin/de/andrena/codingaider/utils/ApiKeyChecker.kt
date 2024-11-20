package de.andrena.codingaider.utils

import com.intellij.openapi.components.Service
import java.io.File

interface ApiKeyChecker {
    fun isApiKeyAvailableForLlm(llm: String): Boolean
    fun isApiKeyAvailable(apiKeyName: String): Boolean
    fun getApiKeyForLlm(llm: String): String?
    fun getAllLlmOptions(): List<String>
    fun getAllApiKeyNames(): List<String>
    fun getApiKeyValue(apiKeyName: String): String?
    fun getApiKeysForDocker(): Map<String, String>
}
@Service(Service.Level.PROJECT)
class DefaultApiKeyChecker : ApiKeyChecker {
    private val llmToApiKeyMap = mapOf(
        "--sonnet" to "ANTHROPIC_API_KEY",
        "--haiku" to "ANTHROPIC_API_KEY",
        "--mini" to "OPENAI_API_KEY",
        "--4o" to "OPENAI_API_KEY",
        "o1-mini" to "OPENAI_API_KEY",
        "o1-preview" to "OPENAI_API_KEY",
        "--deepseek" to "DEEPSEEK_API_KEY"
    )

    private fun getCustomProvider(llm: String): CustomLlmProvider? {
        return service<CustomLlmProviderService>().getProvider(llm)
    }

    private fun getProviderApiKeyName(provider: CustomLlmProvider): String {
        return when (provider.type) {
            LlmProviderType.OPENAI -> "OPENAI_API_KEY"
            LlmProviderType.OPENROUTER -> "OPENROUTER_API_KEY"
            LlmProviderType.OLLAMA -> "" // Ollama doesn't require an API key
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
            if (!customProvider.type.requiresApiKey) {
                return true
            }
            
            // Check for API key
            val apiKeyName = customProvider.type.getApiKeyName(customProvider.name)
            return isApiKeyAvailable(apiKeyName)
        }
        
        return true // If no provider found, assume no API key needed
    }

    override fun isApiKeyAvailable(apiKeyName: String): Boolean {
        return getApiKeyValue(apiKeyName) != null
    }

    override fun getApiKeyForLlm(llm: String): String? {
        // Check standard providers
        llmToApiKeyMap[llm]?.let { return it }
        
        // Check custom providers
        val customProvider = getCustomProvider(llm)
        if (customProvider != null) {
            return customProvider.type.getApiKeyName(customProvider.name)
        }
        
        return null
    }

    override fun getAllLlmOptions(): List<String> {
        val standardOptions = llmToApiKeyMap.keys.toList()
        val customOptions = service<CustomLlmProviderService>().getAllProviders().map { it.name }
        return standardOptions + customOptions
    }

    override fun getAllApiKeyNames(): List<String> = llmToApiKeyMap.values.distinct()

    override fun getApiKeyValue(apiKeyName: String): String? {
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

    override fun getApiKeysForDocker(): Map<String, String> {
        val standardKeys = llmToApiKeyMap.values.distinct()
            .mapNotNull { apiKeyName -> 
                getApiKeyValue(apiKeyName)?.let { apiKeyName to it }
            }
            
        val customKeys = service<CustomLlmProviderService>().getAllProviders()
            .filter { it.type.requiresApiKey }
            .mapNotNull { provider ->
                val keyName = getProviderApiKeyName(provider)
                ApiKeyManager.getCustomModelKey(provider.name)?.let { keyName to it }
            }
            
        return (standardKeys + customKeys).toMap()
    }
}
