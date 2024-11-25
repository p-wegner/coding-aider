package de.andrena.codingaider.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
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
        fun getInstance(): DefaultProviderSettings =
            ApplicationManager.getApplication().getService(DefaultProviderSettings::class.java)

    }
}
