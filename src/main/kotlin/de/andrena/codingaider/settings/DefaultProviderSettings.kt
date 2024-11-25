package de.andrena.codingaider.settings

import com.intellij.openapi.components.*

@State(
    name = "DefaultProviderSettings",
    storages = [Storage("codingAiderDefaultProviders.xml")]
)
data class DefaultProviderSettings(
    var hiddenProviders: MutableSet<String> = mutableSetOf()
) : PersistentStateComponent<DefaultProviderSettings> {

    override fun getState(): DefaultProviderSettings = this

    override fun loadState(state: DefaultProviderSettings) {
        hiddenProviders = state.hiddenProviders
    }

    companion object {
        fun getInstance(): DefaultProviderSettings = service()
    }
}
