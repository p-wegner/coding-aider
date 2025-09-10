package de.andrena.codingaider.settings

import com.intellij.openapi.components.*
import de.andrena.codingaider.inputdialog.AiderMode

/**
 * Aider-specific settings that are only relevant to the Aider CLI tool.
 * This complements the GenericCliSettings with Aider-specific configuration.
 */
@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.AiderSpecificSettings",
    storages = [Storage("AiderSpecificSettings.xml", roamingType = RoamingType.DISABLED)]
)
class AiderSpecificSettings : PersistentStateComponent<AiderSpecificSettings.State>, CliSpecificSettings {
    
    data class State(
        /**
         * Aider executable path
         */
        var aiderExecutablePath: String = "aider",
        
        /**
         * Reasoning effort level
         */
        var reasoningEffort: String = "normal",
        
        /**
         * Edit format specification
         */
        var editFormat: String = "",
        
        /**
         * Lint command
         */
        var lintCmd: String = "",
        
        /**
         * Whether to deactivate repository map
         */
        var deactivateRepoMap: Boolean = false,
        
        /**
         * Whether to include change context
         */
        var includeChangeContext: Boolean = false,
        
        /**
         * Whether to use sidecar mode
         */
        var useSidecarMode: Boolean = false,
        
        /**
         * Auto-commit settings
         */
        var autoCommits: AutoCommitSetting = AutoCommitSetting.DISABLED,
        
        /**
         * Dirty commit settings
         */
        var dirtyCommits: DirtyCommitSetting = DirtyCommitSetting.DISABLED,
        
        /**
         * Plugin-based edits settings
         */
        var pluginBasedEdits: Boolean = true,
        
        /**
         * Whether to allow lenient edits
         */
        var lenientEdits: Boolean = true,
        
        /**
         * Whether to auto-commit after plugin-based edits
         */
        var autoCommitAfterEdits: Boolean = true,
        
        /**
         * Default Aider mode
         */
        var defaultAiderMode: AiderMode = AiderMode.NORMAL,
        
        /**
         * Aider-specific Docker settings
         */
        var dockerImage: String = "paulgauthier/aider",
        
        /**
         * Aider-specific additional arguments
         */
        var aiderAdditionalArgs: String = "",
        
        /**
         * Aider-specific flags
         */
        var aiderFlags: AiderFlags = AiderFlags()
    )
    
    data class AiderFlags(
        /**
         * Whether to disable suggest shell commands
         */
        var disableSuggestShellCommands: Boolean = true,
        
        /**
         * Whether to disable pretty output
         */
        var disablePretty: Boolean = true,
        
        /**
         * Whether to disable weak model confirmation
         */
        var disableWeakModelConfirmation: Boolean = false,
        
        /**
         * Whether to suggest shell commands
         */
        var suggestShellCommands: Boolean = false,
        
        /**
         * Whether to use pretty output
         */
        var pretty: Boolean = false,
        
        /**
         * Whether to enable weak model confirmation
         */
        var weakModelConfirmation: Boolean = false,
        
        /**
         * Whether to enable cache prompts
         */
        var cachePrompts: Boolean = false,
        
        /**
         * Whether to enable cache repository map
         */
        var cacheRepositoryMap: Boolean = false
    )
    
    /**
     * Auto-commit setting enum
     */
    enum class AutoCommitSetting {
        ENABLED,
        DISABLED,
        ASK
    }
    
    /**
     * Dirty commit setting enum
     */
    enum class DirtyCommitSetting {
        ENABLED,
        DISABLED,
        ASK
    }
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    // Property delegates
    var aiderExecutablePath: String
        get() = myState.aiderExecutablePath
        set(value) { myState.aiderExecutablePath = value }
    
    var reasoningEffort: String
        get() = myState.reasoningEffort
        set(value) { myState.reasoningEffort = value }
    
    var editFormat: String
        get() = myState.editFormat
        set(value) { myState.editFormat = value }
    
    var lintCmd: String
        get() = myState.lintCmd
        set(value) { myState.lintCmd = value }
    
    var deactivateRepoMap: Boolean
        get() = myState.deactivateRepoMap
        set(value) { myState.deactivateRepoMap = value }
    
    var includeChangeContext: Boolean
        get() = myState.includeChangeContext
        set(value) { myState.includeChangeContext = value }
    
    var autoCommits: AutoCommitSetting
        get() = myState.autoCommits
        set(value) { myState.autoCommits = value }
    
    var dirtyCommits: DirtyCommitSetting
        get() = myState.dirtyCommits
        set(value) { myState.dirtyCommits = value }
    
    var pluginBasedEdits: Boolean
        get() = myState.pluginBasedEdits
        set(value) { myState.pluginBasedEdits = value }
    
    var lenientEdits: Boolean
        get() = myState.lenientEdits
        set(value) { myState.lenientEdits = value }
    
    var autoCommitAfterEdits: Boolean
        get() = myState.autoCommitAfterEdits
        set(value) { myState.autoCommitAfterEdits = value }
    
    var defaultAiderMode: AiderMode
        get() = myState.defaultAiderMode
        set(value) { myState.defaultAiderMode = value }
    
    var dockerImage: String
        get() = myState.dockerImage
        set(value) { myState.dockerImage = value }
    
    var aiderAdditionalArgs: String
        get() = myState.aiderAdditionalArgs
        set(value) { myState.aiderAdditionalArgs = value }
    
    var aiderFlags: AiderFlags
        get() = myState.aiderFlags
        set(value) { myState.aiderFlags = value }
    
    // CliSpecificSettings implementation
    override fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (aiderExecutablePath.isBlank()) {
            errors.add("Aider executable path cannot be blank")
        }
        
        if (reasoningEffort.isBlank()) {
            errors.add("Reasoning effort cannot be blank")
        }
        
        return errors
    }
    
    override fun getExecutablePath(): String {
        return aiderExecutablePath
    }
    
    override fun getAdditionalArgs(): String {
        return aiderAdditionalArgs
    }
    
    companion object {
        fun getInstance(): AiderSpecificSettings {
            return com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(AiderSpecificSettings::class.java)
        }
    }
}