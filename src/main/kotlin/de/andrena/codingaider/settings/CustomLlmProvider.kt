package de.andrena.codingaider.settings

data class CustomLlmProvider(
    var name: String,
    var displayName: String? = null,
    var type: LlmProviderType = LlmProviderType.OPENAI,
    var baseUrl: String = "",
    var modelName: String = ""
) {
    val prefixedModelName: String
        get() {
            if (modelName.startsWith("${type.modelNamePrefix}/")) return modelName
            return "${type.modelNamePrefix}/${modelName}"
        }
}
