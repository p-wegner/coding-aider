package de.andrena.codingaider.utils

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class EnvironmentVariableManager {
    
    private val envVarCache = ConcurrentHashMap<String, CachedEnvironmentVariable>()
    private val CACHE_DURATION = Duration.ofMinutes(10) // Longer cache for env vars
    
    private data class CachedEnvironmentVariable(
        val value: String?,
        val timestamp: Instant = Instant.now()
    )
    
    companion object {
        fun getInstance(): EnvironmentVariableManager = service()
    }
    
    /**
     * Get environment variable value with caching and fallback to .env files
     */
    fun getEnvironmentVariable(name: String): String? {
        if (name.isBlank()) return null
        
        // Check cache first
        envVarCache[name]?.let { cached ->
            if (Duration.between(cached.timestamp, Instant.now()) < CACHE_DURATION) {
                return cached.value
            }
        }
        
        // Retrieve environment variable
        val value = retrieveEnvironmentVariable(name)
        
        // Cache the result (including null values)
        envVarCache[name] = CachedEnvironmentVariable(value)
        
        return value
    }
    
    /**
     * Check if environment variable is available
     */
    fun isEnvironmentVariableAvailable(name: String): Boolean {
        return getEnvironmentVariable(name) != null
    }
    
    /**
     * Check if any of the provided environment variables are available
     */
    fun areAnyEnvironmentVariablesAvailable(names: List<String>): Boolean {
        return names.any { isEnvironmentVariableAvailable(it) }
    }
    
    /**
     * Get the first available environment variable from a list
     */
    fun getFirstAvailableEnvironmentVariable(names: List<String>): String? {
        return names.firstNotNullOfOrNull { getEnvironmentVariable(it) }
    }
    
    /**
     * Clear cache for specific environment variable
     */
    fun clearCacheForVariable(name: String) {
        envVarCache.remove(name)
    }
    
    /**
     * Clear entire cache
     */
    fun clearCache() {
        envVarCache.clear()
    }
    
    private fun retrieveEnvironmentVariable(name: String): String? {
        // Check system environment variable first
        System.getenv(name)?.let { return it }
        
        // Check .env file in the user's home directory
        val homeEnvFile = File(System.getProperty("user.home"), ".env")
        readEnvFile(homeEnvFile)[name]?.let { return it }
        
        // Check .env file in the current working directory
        val currentDirEnvFile = File(System.getProperty("user.dir"), ".env")
        readEnvFile(currentDirEnvFile)[name]?.let { return it }
        
        return null
    }
    
    private fun readEnvFile(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return try {
            file.readLines()
                .filter { it.contains('=') && !it.trim().startsWith('#') }
                .associate { line ->
                    val (key, value) = line.split('=', limit = 2)
                    key.trim() to value.trim().removeSurrounding("\"").removeSurrounding("'")
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
