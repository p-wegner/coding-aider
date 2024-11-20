package de.andrena.codingaider.settings

data class CustomModelSettings(
    val apiBaseUrl: String = "",
    val modelName: String = "",
    var apiKey: String = ""
) {
    fun isConfigured(): Boolean {
        return apiBaseUrl.isNotBlank() && 
               modelName.startsWith("openai/") && 
               modelName.length > 7 && 
               apiKey.isNotBlank()
    }
}
