package de.andrena.codingaider.settings

data class CustomModelSettings(
    val apiBaseUrl: String = "",
    val modelName: String = "",
    var apiKey: String = ""
) {
    fun isValid(): Boolean {
        return apiBaseUrl.isNotBlank() && 
               modelName.startsWith("openai/") && 
               modelName.length > 7 && // More than just "openai/"
               apiKey.isNotBlank()
    }
}
