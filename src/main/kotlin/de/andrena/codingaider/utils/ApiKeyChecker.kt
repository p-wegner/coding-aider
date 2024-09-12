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

    fun isApiKeyAvailableForLlm(llm: String): Boolean {
        val apiKey = llmToApiKeyMap[llm] ?: return true // If no API key is needed, consider it available
        return isApiKeyAvailable(apiKey)
    }

    private fun isApiKeyAvailable(apiKeyName: String): Boolean {
        return getApiKeyValue(apiKeyName) != null
    }

    fun getApiKeyForLlm(llm: String): String? = llmToApiKeyMap[llm]

    fun getAllLlmOptions(): List<String> = llmToApiKeyMap.keys.toList() + ""

    fun getAllApiKeyNames(): List<String> = llmToApiKeyMap.values.distinct()

    fun getApiKeyValue(apiKeyName: String): String? {
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

    fun getApiKeysForDocker(): Map<String, String> {
        return llmToApiKeyMap.values.distinct().mapNotNull { apiKeyName ->
            getApiKeyValue(apiKeyName)?.let { apiKeyName to it }
        }.toMap()
    }
}
