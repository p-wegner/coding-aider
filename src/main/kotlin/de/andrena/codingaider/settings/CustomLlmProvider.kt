package de.andrena.codingaider.settings

data class CustomLlmProvider(
    var name: String = "",
    var displayName: String? = null,
    var type: LlmProviderType = LlmProviderType.OPENAI,
    var baseUrl: String = "",
    var modelName: String = ""
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) {
            errors.add("Provider name is required")
        }
        if (modelName.isBlank()) {
            errors.add("Model name is required")
        }
        if (type.requiresBaseUrl && baseUrl.isBlank()) {
            errors.add("Base URL is required for ${type.displayName}")
        }
        
        return errors
    }
}
