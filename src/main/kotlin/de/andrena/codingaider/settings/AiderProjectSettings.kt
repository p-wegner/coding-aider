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
        var persistentFiles: List<FileData> = emptyList()
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

    companion object {
        fun getInstance(project: Project): AiderProjectSettings =
            project.getService(AiderProjectSettings::class.java)
    }
}
