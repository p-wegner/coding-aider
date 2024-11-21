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

}
