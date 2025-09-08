package de.andrena.codingaider.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.ApiKeyManager

@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.CustomLlmProviderService",
    storages = [Storage("customLlmProviders.xml")]
)
class CustomLlmProviderService : PersistentStateComponent<CustomLlmProviderService.State> {

    class State {
        var providers: MutableList<CustomLlmProvider> = mutableListOf()
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    private val settingsChangeListeners = mutableListOf<() -> Unit>()

    fun addSettingsChangeListener(listener: () -> Unit) {
        settingsChangeListeners.add(listener)
    }

    fun removeSettingsChangeListener(listener: () -> Unit) {
        settingsChangeListeners.remove(listener)
    }

    private fun notifySettingsChanged() {
        settingsChangeListeners.forEach { it() }
    }

    fun addProvider(provider: CustomLlmProvider) {
        myState.providers.add(provider)
        notifySettingsChanged()
    }

    fun removeProvider(name: String) {
        myState.providers.removeIf { it.name == name }
        // Also remove the API key when provider is removed
        ApiKeyManager.removeCustomModelKey(name)
        notifySettingsChanged()
    }

    fun getProvider(name: String): CustomLlmProvider? {
        return myState.providers.find { it.name == name }
    }

    fun getAllProviders(): List<CustomLlmProvider> = myState.providers.toList()

    fun getVisibleProviders(): List<CustomLlmProvider> = myState.providers.filter { !it.hidden }

    fun toggleProviderVisibility(name: String) {
        val provider = getProvider(name) ?: return
        val index = myState.providers.indexOfFirst { it.name == name }
        if (index >= 0) {
            myState.providers[index] = provider.copy(hidden = !provider.hidden)
            notifySettingsChanged()
        }
    }


}
