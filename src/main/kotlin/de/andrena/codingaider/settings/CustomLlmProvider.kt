package de.andrena.codingaider.settings

data class CustomLlmProvider(
    var name: String = "",
    var type: LlmProviderType = LlmProviderType.OPENAI,
    var baseUrl: String = "",
    var modelName: String = "",
    var hidden: Boolean = false,
    var isBuiltIn: Boolean = false,
    var projectId: String = "",  // For Vertex AI: GCP project ID
    var location: String = "",   // For Vertex AI: GCP region
    var hasExplicitApiKey: Boolean = false,  // Track if API key was explicitly provided
    var customEnvironmentVariable: String = ""  // Custom environment variable name
) {
    
    /**
     * Check if this provider has authentication available
     */
    fun hasAuthenticationAvailable(): Boolean {
        if (!type.supportsApiKey) return true
        
        // Check if explicit API key is available
        val customKey = ApiKeyManager.getCustomModelKey(name)
        if (customKey != null && customKey.isNotBlank()) {
            return true
        }
        
        // For optional API keys, check environment variables
        if (type.apiKeyOptional) {
            val envManager = com.intellij.openapi.components.service<de.andrena.codingaider.utils.EnvironmentVariableManager>()
            
            // Check custom environment variable first
            if (customEnvironmentVariable.isNotBlank() && envManager.isEnvironmentVariableAvailable(customEnvironmentVariable)) {
                return true
            }
            
            // Check default environment variables
            return envManager.areAnyEnvironmentVariablesAvailable(type.getDefaultEnvironmentVariables())
        }
        
        // For required API keys, check if key is available
        return customKey != null && customKey.isNotBlank()
    }
    
    /**
     * Get the effective API key for this provider
     */
    fun getEffectiveApiKey(): String? {
        if (!type.supportsApiKey) return null
        
        // Check explicit API key first
        val customKey = ApiKeyManager.getCustomModelKey(name)
        if (customKey != null && customKey.isNotBlank()) {
            return customKey
        }
        
        // For optional API keys, check environment variables
        if (type.apiKeyOptional) {
            val envManager = com.intellij.openapi.components.service<de.andrena.codingaider.utils.EnvironmentVariableManager>()
            
            // Check custom environment variable first
            if (customEnvironmentVariable.isNotBlank()) {
                envManager.getEnvironmentVariable(customEnvironmentVariable)?.let { return it }
            }
            
            // Check default environment variables
            return envManager.getFirstAvailableEnvironmentVariable(type.getDefaultEnvironmentVariables())
        }
        
        return customKey
    }
}
