package de.andrena.codingaider.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.ApiKeyManager

@Service(Service.Level.APP)
@State(
    name = "CustomLlmProviderService",
    storages = [Storage("customLlmProviders.xml")]
)
class CustomLlmProviderService : PersistentStateComponent<CustomLlmProviderService.State> {
    
    data class State(
        var providers: MutableList<CustomLlmProvider> = mutableListOf()
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun addProvider(provider: CustomLlmProvider) {
        myState.providers.add(provider)
    }

    fun removeProvider(name: String) {
        myState.providers.removeIf { it.name == name }
        // Also remove the API key when provider is removed
        ApiKeyManager.removeCustomModelKey(name)
    }

    fun getProvider(name: String): CustomLlmProvider? {
        return myState.providers.find { it.name == name }
    }

    fun getAllProviders(): List<CustomLlmProvider> = myState.providers.toList()

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CustomLlmProviderService {
            return project.service<CustomLlmProviderService>()
        }
    }
}
