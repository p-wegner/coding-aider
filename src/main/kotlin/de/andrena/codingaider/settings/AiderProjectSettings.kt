package de.andrena.codingaider.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData

@Service(Service.Level.PROJECT)
@State(
    name = "de.andrena.codingaider.settings.AiderProjectSettings",
    storages = [Storage("AiderProjectSettings.xml")]
)
class AiderProjectSettings(private val project: Project) : PersistentStateComponent<AiderProjectSettings.State> {
    data class State(
        var persistentFiles: List<FileData> = emptyList(),
        var isOptionsCollapsed: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var persistentFiles: List<FileData>
        get() = myState.persistentFiles
        set(value) {
            myState.persistentFiles = value
        }

    var isOptionsCollapsed: Boolean
        get() = myState.isOptionsCollapsed
        set(value) {
            myState.isOptionsCollapsed = value
        }

    companion object {
        fun getInstance(project: Project): AiderProjectSettings =
            project.getService(AiderProjectSettings::class.java)
    }
}
package de.andrena.codingaider.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "AiderProjectSettings",
    storages = [Storage("aiderProjectSettings.xml")]
)
class AiderProjectSettings : PersistentStateComponent<AiderProjectSettings.State> {
    data class State(
        var isOptionsCollapsed: Boolean = false,
        var isContextCollapsed: Boolean = false
    )

    private var myState = State()

    var isOptionsCollapsed: Boolean
        get() = myState.isOptionsCollapsed
        set(value) {
            myState.isOptionsCollapsed = value
        }

    var isContextCollapsed: Boolean
        get() = myState.isContextCollapsed
        set(value) {
            myState.isContextCollapsed = value
        }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): AiderProjectSettings =
            project.getService(AiderProjectSettings::class.java)
    }
}
