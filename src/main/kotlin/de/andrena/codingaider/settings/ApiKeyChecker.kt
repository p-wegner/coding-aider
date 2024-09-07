package de.andrena.codingaider.settings

import java.io.File
import java.nio.file.Paths

object ApiKeyChecker {
    private val apiKeyMap = mapOf(
        "OPENAI_API_KEY" to listOf("mini", "4o"),
        "ANTHROPIC_API_KEY" to listOf("sonnet"),
        "DEEPSEEK_API_KEY" to listOf("deepseek")
    )

    fun checkApiKeys(): Map<String, Boolean> {
        return apiKeyMap.mapValues { (key, _) ->
            isApiKeyAvailable(key)
        }
    }

    private fun isApiKeyAvailable(apiKeyName: String): Boolean {
        // Check environment variable
        if (System.getenv(apiKeyName) != null) return true

        // Check .env file in the user's home directory
        val homeDir = System.getProperty("user.home")
        val envFile = File(homeDir, ".env")
        if (envFile.exists() && envFile.readText().contains("$apiKeyName=")) return true

        // Check .env file in the current working directory
        val currentDir = Paths.get("").toAbsolutePath().toString()
        val currentEnvFile = File(currentDir, ".env")
        if (currentEnvFile.exists() && currentEnvFile.readText().contains("$apiKeyName=")) return true

        return false
    }

    fun getLlmForApiKey(apiKey: String): List<String> {
        return apiKeyMap[apiKey] ?: emptyList()
    }

    fun getAllLlmOptions(): List<String> {
        return apiKeyMap.values.flatten().distinct()
    }

    fun isApiKeyAvailableForLlm(llm: String): Boolean {
        val apiKey = apiKeyMap.entries.find { (_, llms) -> llm in llms }?.key
        return apiKey?.let { isApiKeyAvailable(it) } ?: false
    }

    fun getApiKeyForLlm(llm: String): String? {
        return apiKeyMap.entries.find { (_, llms) -> llm in llms }?.key
    }
}
