package de.andrena.codingaider.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import de.andrena.codingaider.features.documentation.DefaultDocumentTypes
import de.andrena.codingaider.features.documentation.DocumentTypeConfiguration
import de.andrena.codingaider.features.testgeneration.DefaultTestTypes
import de.andrena.codingaider.features.testgeneration.TestTypeConfiguration
import de.andrena.codingaider.features.customactions.DefaultCustomActions
import de.andrena.codingaider.features.customactions.CustomActionConfiguration

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
        var plansFolderPath: String? = null,
        var testTypes: MutableList<TestTypeConfiguration> = DefaultTestTypes.getDefaultTestTypes().toMutableList(),
        var documentTypes: MutableList<DocumentTypeConfiguration> = DefaultDocumentTypes.getDefaultDocumentTypes().toMutableList(),
        var customActions: MutableList<CustomActionConfiguration> = DefaultCustomActions.getDefaultCustomActions().toMutableList()
    )

    private var myState = State()
    private val settingsChangeListeners = mutableListOf<() -> Unit>()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun addSettingsChangeListener(listener: () -> Unit) {
        settingsChangeListeners.add(listener)
    }

    fun removeSettingsChangeListener(listener: () -> Unit) {
        settingsChangeListeners.remove(listener)
    }

    private fun notifySettingsChanged() {
        settingsChangeListeners.forEach { it() }
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

    var plansFolderPath: String?
        get() = myState.plansFolderPath
        set(value) {
            val oldValue = myState.plansFolderPath
            myState.plansFolderPath = value
            if (oldValue != value) {
                notifySettingsChanged()
            }
        }

    // Test Types methods
    fun getTestTypes(): List<TestTypeConfiguration> = myState.testTypes.toList()

    fun addTestType(testType: TestTypeConfiguration) {
        val relativePathTestType = testType.withRelativePaths(project.basePath ?: "")
        myState.testTypes.add(relativePathTestType)
    }

    fun updateTestType(index: Int, testType: TestTypeConfiguration) {
        if (index in 0 until myState.testTypes.size) {
            val relativePathTestType = testType.withRelativePaths(project.basePath ?: "")
            myState.testTypes[index] = relativePathTestType
        }
    }

    fun removeTestType(index: Int) {
        if (index in 0 until myState.testTypes.size) {
            myState.testTypes.removeAt(index)
        }
    }

    // Document Types methods
    fun getDocumentTypes(): List<DocumentTypeConfiguration> = myState.documentTypes.toList()

    fun addDocumentType(documentType: DocumentTypeConfiguration) {
        val relativePathDocumentType = documentType.withRelativePaths(project.basePath ?: "")
        myState.documentTypes.add(relativePathDocumentType)
    }

    fun updateDocumentType(index: Int, documentType: DocumentTypeConfiguration) {
        if (index in 0 until myState.documentTypes.size) {
            val relativePathDocumentType = documentType.withRelativePaths(project.basePath ?: "")
            myState.documentTypes[index] = relativePathDocumentType
        }
    }

    fun removeDocumentType(index: Int) {
        if (index in 0 until myState.documentTypes.size) {
            myState.documentTypes.removeAt(index)
        }
    }

    // Custom Actions methods
    fun getCustomActions(): List<CustomActionConfiguration> = myState.customActions.toList()

    fun addCustomAction(customAction: CustomActionConfiguration) {
        val relativePathCustomAction = customAction.withRelativePaths(project.basePath ?: "")
        myState.customActions.add(relativePathCustomAction)
    }

    fun updateCustomAction(index: Int, customAction: CustomActionConfiguration) {
        if (index in 0 until myState.customActions.size) {
            val relativePathCustomAction = customAction.withRelativePaths(project.basePath ?: "")
            myState.customActions[index] = relativePathCustomAction
        }
    }

    fun removeCustomAction(index: Int) {
        if (index in 0 until myState.customActions.size) {
            myState.customActions.removeAt(index)
        }
    }

    companion object {
        fun getInstance(project: Project): AiderProjectSettings =
            project.getService(AiderProjectSettings::class.java)
    }
}


