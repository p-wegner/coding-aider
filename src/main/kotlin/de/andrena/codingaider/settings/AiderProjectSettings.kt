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
    data class TestTypeConfiguration(
        var name: String = "",
        var promptTemplate: String = "",
        var referenceFilePattern: String = "",
        var testFilePattern: String = "*Test.kt",
        var isEnabled: Boolean = true,
        var contextFiles: List<String> = listOf()
    )

    data class State(
        var isOptionsCollapsed: Boolean = true,
        var isContextCollapsed: Boolean = false,
        var workingDirectory: String? = null,
        var testTypes: MutableList<TestTypeConfiguration> = mutableListOf(
            TestTypeConfiguration(
                name = "Unit Test",
                promptTemplate = """Generate a unit test for the provided code following these guidelines:
                    |1. Follow the given/when/then pattern
                    |2. Include edge cases and error scenarios
                    |3. Try to adhere to the DRY and KISS principles, e.g. by using helper methods
                    |4. Use descriptive test method names
                    |5. Add comments explaining complex test scenarios""".trimMargin(),
                referenceFilePattern = ".*Test\\.(kt|java|ts|cs|py|go)$",
                testFilePattern = "*Test.{kt,java,ts,cs,py,go}",
                isEnabled = true
            ),
            TestTypeConfiguration(
                name = "Integration Test",
                promptTemplate = """Generate an integration test focusing on component interaction:
                    |1. Test the integration between multiple components
                    |2. Include setup and teardown of resources
                    |3. Test both success and failure scenarios
                    |4. Use appropriate mocking where needed
                    |5. Consider transaction boundaries""".trimMargin(),
                referenceFilePattern = ".*IT\\.(kt|java|ts|cs|py|go)$",
                testFilePattern = "*IT.{kt,java,ts,cs,py,go}",
                isEnabled = true
            ),
            TestTypeConfiguration(
                name = "BDD Specification",
                promptTemplate = """Generate a BDD-style test specification:
                    |1. Use descriptive feature/scenario language
                    |2. Follow the given/when/then pattern strictly
                    |3. Include scenario outlines for parameterized tests
                    |4. Add clear documentation comments
                    |5. Focus on business requirements""".trimMargin(),
                referenceFilePattern = ".*Spec\\.(kt|java|ts|cs|py|go)$",
                testFilePattern = "*Spec.{kt,java,ts,cs,py,go}",
                isEnabled = true
            )
        )
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
