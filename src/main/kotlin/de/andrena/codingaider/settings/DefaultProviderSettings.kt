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

    private val listeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    fun isProviderHidden(providerName: String): Boolean {
        return hiddenProviders.contains(providerName)
    }

    override fun getState(): DefaultProviderSettings = this

    override fun loadState(state: DefaultProviderSettings) {
        hiddenProviders = state.hiddenProviders
        notifyListeners()
    }

    companion object {
        fun getInstance(): DefaultProviderSettings =
            ApplicationManager.getApplication().getService(DefaultProviderSettings::class.java)
    }
}
