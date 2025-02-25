package de.andrena.codingaider.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import de.andrena.codingaider.settings.testgeneration.DefaultTestTypes
import de.andrena.codingaider.settings.testgeneration.TestTypeConfiguration

@Service(Service.Level.PROJECT)
@State(
    name = "de.andrena.codingaider.settings.AiderProjectSettings",
    storages = [Storage("AiderProjectSettings.xml")]
)
class AiderProjectSettings(private val project: Project) : PersistentStateComponent<AiderProjectSettings.State> {
    data class State(
        var isOptionsCollapsed: Boolean = true,
        var isContextCollapsed: Boolean = false,
        var workingDirectory: String? = null,
        var testTypes: MutableList<TestTypeConfiguration> = DefaultTestTypes.getDefaultTestTypes().toMutableList()
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

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

    var workingDirectory: String?
        get() = myState.workingDirectory
        set(value) {
            myState.workingDirectory = value
        }

    fun getTestTypes(): List<TestTypeConfiguration> = myState.testTypes.toList()

    fun addTestType(testType: TestTypeConfiguration) {
        myState.testTypes.add(testType)
    }

    fun updateTestType(index: Int, testType: TestTypeConfiguration) {
        if (index in 0 until myState.testTypes.size) {
            myState.testTypes[index] = testType
        }
    }

    fun removeTestType(index: Int) {
        if (index in 0 until myState.testTypes.size) {
            myState.testTypes.removeAt(index)
        }
    }

    companion object {
        fun getInstance(project: Project): AiderProjectSettings =
            project.getService(AiderProjectSettings::class.java)
    }

}


