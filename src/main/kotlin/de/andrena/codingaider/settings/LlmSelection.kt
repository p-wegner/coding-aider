package de.andrena.codingaider.settings

data class LlmSelection(
    val name: String,
    val displayName: String? = null,
    val provider: CustomLlmProvider? = null,
    val isBuiltIn: Boolean = true
) {
    fun getDisplayText(): String {
        return when {
            provider != null -> provider.displayName?.ifEmpty { provider.name } ?: provider.name
            displayName != null -> displayName
            else -> name
        }
    }

    override fun toString(): String = name

    companion object {
        fun fromString(value: String, customProviderService: CustomLlmProviderService): LlmSelection {
            val customProvider = customProviderService.getProvider(value)
            return if (customProvider != null) {
                LlmSelection(
                    name = value,
                    provider = customProvider,
                    isBuiltIn = false
                )
            } else {
                LlmSelection(name = value)
            }
        }
    }
}
