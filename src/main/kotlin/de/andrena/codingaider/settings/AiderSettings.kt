package de.andrena.codingaider.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(
    name = "de.andrena.codingaider.settings.AiderSettings",
    storages = [Storage("AiderSettings.xml")]
)
class AiderSettings : PersistentStateComponent<AiderSettings.State> {
    data class State(
        var useYesFlag: Boolean = AiderDefaults.USE_YES_FLAG,
        var llm: String = AiderDefaults.LLM,
        var additionalArgs: String = AiderDefaults.ADDITIONAL_ARGS,
        var isShellMode: Boolean = AiderDefaults.IS_SHELL_MODE,
        var lintCmd: String = AiderDefaults.LINT_CMD
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var useYesFlag: Boolean
        get() = myState.useYesFlag
        set(value) {
            myState.useYesFlag = value
        }

    var llm: String
        get() = myState.llm
        set(value) {
            myState.llm = value
        }

    var additionalArgs: String
        get() = myState.additionalArgs
        set(value) {
            myState.additionalArgs = value
        }

    var isShellMode: Boolean
        get() = myState.isShellMode
        set(value) {
            myState.isShellMode = value
        }

    var lintCmd: String
        get() = myState.lintCmd
        set(value) {
            myState.lintCmd = value
        }

    companion object {
        fun getInstance(project: Project): AiderSettings =
            project.getService(AiderSettings::class.java)
    }
}
