package de.andrena.codingaider.settings

data class CustomLlmProvider(
    var name: String = "",
    var type: LlmProviderType = LlmProviderType.OPENAI,
    var baseUrl: String = "",
    var modelName: String = "",
    var hidden: Boolean = false,
    var isBuiltIn: Boolean = false,
    var projectId: String = "",  // For Vertex AI: GCP project ID
    var location: String = ""    // For Vertex AI: GCP region
) {
}
