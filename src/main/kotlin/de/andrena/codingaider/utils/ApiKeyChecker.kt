package de.andrena.codingaider.utils

import java.io.File
import java.nio.file.Paths

object ApiKeyChecker {
    private val llmToApiKeyMap = mapOf(
        "--sonnet" to "ANTHROPIC_API_KEY",
        "--mini" to "OPENAI_API_KEY",
        "--4o" to "OPENAI_API_KEY",
        "--deepseek" to "DEEPSEEK_API_KEY"
    )

    fun checkApiKeys(): Map<String, Boolean> {
        return llmToApiKeyMap.values.distinct().associateWith { isApiKeyAvailable(it) }
    }

    fun isApiKeyAvailableForLlm(llm: String): Boolean {
        val apiKey = llmToApiKeyMap[llm] ?: return true // If no API key is needed, consider it available
        return isApiKeyAvailable(apiKey)
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

    fun getApiKeyForLlm(llm: String): String? = llmToApiKeyMap[llm]

    fun getAllLlmOptions(): List<String> = llmToApiKeyMap.keys.toList() + ""
}
