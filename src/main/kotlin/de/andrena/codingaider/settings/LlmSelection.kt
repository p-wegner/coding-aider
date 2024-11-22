package de.andrena.codingaider.settings

data class LlmSelection(
    val name: String,
    val provider: CustomLlmProvider? = null,
    val isBuiltIn: Boolean = true
) {
    fun getDisplayText(): String {
        return when {
            provider != null ->  provider.name
            else -> name
        }
    }

    override fun toString(): String = name

}
