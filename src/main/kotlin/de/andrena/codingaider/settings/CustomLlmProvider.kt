package de.andrena.codingaider.settings

data class CustomLlmProvider(
    var name: String = "",
    var type: LlmProviderType = LlmProviderType.OPENAI,
    var baseUrl: String = "",
    var modelName: String = "",
    var hidden: Boolean = false
) {
    val prefixedModelName: String
        get() {
            if (modelName.startsWith("${type.modelNamePrefix}/")) return modelName
            return "${type.modelNamePrefix}/${modelName}"
        }
}
