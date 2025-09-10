package de.andrena.codingaider.settings

import com.intellij.openapi.components.*

/**
 * Claude Code-specific settings that are only relevant to the Claude Code CLI tool.
 * This complements the GenericCliSettings with Claude Code-specific configuration.
 */
@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.ClaudeCodeSpecificSettings",
    storages = [Storage("ClaudeCodeSpecificSettings.xml", roamingType = RoamingType.DISABLED)]
)
class ClaudeCodeSpecificSettings : PersistentStateComponent<ClaudeCodeSpecificSettings.State>, CliSpecificSettings {
    
    data class State(
        /**
         * Claude Code executable path
         */
        var claudeExecutablePath: String = "claude",
        
        /**
         * Maximum tokens for generation
         */
        var maxTokens: Int = 4000,
        
        /**
         * Temperature parameter for generation
         */
        var temperature: Double = 0.7,
        
        /**
         * Top-p parameter for generation
         */
        var topP: Double = 1.0,
        
        /**
         * Whether to enable streaming responses
         */
        var enableStreaming: Boolean = true,
        
        /**
         * Whether to enable thinking mode
         */
        var enableThinking: Boolean = false,
        
        /**
         * Whether to enable verbose output
         */
        var verboseOutput: Boolean = false,
        
        /**
         * Whether to enable edit mode
         */
        var enableEditMode: Boolean = true,
        
        /**
         * Whether to enable file operations
         */
        var enableFileOperations: Boolean = true,
        
        /**
         * Whether to enable shell commands
         */
        var enableShellCommands: Boolean = false,
        
        /**
         * Whether to enable web search
         */
        var enableWebSearch: Boolean = false,
        
        /**
         * System prompt for Claude Code
         */
        var systemPrompt: String = "",
        
        /**
         * User context for Claude Code
         */
        var userContext: String = "",
        
        /**
         * Claude Code-specific additional arguments
         */
        var claudeAdditionalArgs: String = "",
        
        /**
         * Claude Code-specific flags
         */
        var claudeFlags: ClaudeFlags = ClaudeFlags()
    )
    
    data class ClaudeFlags(
        /**
         * Whether to enable safe mode
         */
        var safeMode: Boolean = true,
        
        /**
         * Whether to enable interactive mode
         */
        var interactiveMode: Boolean = true,
        
        /**
         * Whether to enable auto-save
         */
        var autoSave: Boolean = true,
        
        /**
         * Whether to enable backup creation
         */
        var createBackups: Boolean = true,
        
        /**
         * Whether to enable syntax highlighting
         */
        var syntaxHighlighting: Boolean = true,
        
        /**
         * Whether to enable line numbers
         */
        var lineNumbers: Boolean = true,
        
        /**
         * Whether to enable word wrap
         */
        var wordWrap: Boolean = true,
        
        /**
         * Whether to enable minimap
         */
        var minimap: Boolean = true,
        
        /**
         * Whether to enable breadcrumb navigation
         */
        var breadcrumbNavigation: Boolean = true,
        
        /**
         * Whether to enable code folding
         */
        var codeFolding: Boolean = true,
        
        /**
         * Whether to enable auto-completion
         */
        var autoComplete: Boolean = true,
        
        /**
         * Whether to enable error highlighting
         */
        var errorHighlighting: Boolean = true,
        
        /**
         * Whether to enable warning highlighting
         */
        var warningHighlighting: Boolean = true,
        
        /**
         * Whether to enable information tooltips
         */
        var informationTooltips: Boolean = true
    )
    
    /**
     * Output format enum for Claude Code
     */
    enum class OutputFormat {
        MARKDOWN,
        PLAIN_TEXT,
        JSON,
        XML,
        HTML
    }
    
    /**
     * Theme enum for Claude Code
     */
    enum class Theme {
        AUTO,
        LIGHT,
        DARK,
        HIGH_CONTRAST
    }
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    // Property delegates
    var claudeExecutablePath: String
        get() = myState.claudeExecutablePath
        set(value) { myState.claudeExecutablePath = value }
    
    var maxTokens: Int
        get() = myState.maxTokens
        set(value) { myState.maxTokens = value }
    
    var temperature: Double
        get() = myState.temperature
        set(value) { myState.temperature = value }
    
    var topP: Double
        get() = myState.topP
        set(value) { myState.topP = value }
    
    var enableStreaming: Boolean
        get() = myState.enableStreaming
        set(value) { myState.enableStreaming = value }
    
    var enableThinking: Boolean
        get() = myState.enableThinking
        set(value) { myState.enableThinking = value }
    
    var verboseOutput: Boolean
        get() = myState.verboseOutput
        set(value) { myState.verboseOutput = value }
    
    var enableEditMode: Boolean
        get() = myState.enableEditMode
        set(value) { myState.enableEditMode = value }
    
    var enableFileOperations: Boolean
        get() = myState.enableFileOperations
        set(value) { myState.enableFileOperations = value }
    
    var enableShellCommands: Boolean
        get() = myState.enableShellCommands
        set(value) { myState.enableShellCommands = value }
    
    var enableWebSearch: Boolean
        get() = myState.enableWebSearch
        set(value) { myState.enableWebSearch = value }
    
    var systemPrompt: String
        get() = myState.systemPrompt
        set(value) { myState.systemPrompt = value }
    
    var userContext: String
        get() = myState.userContext
        set(value) { myState.userContext = value }
    
    var claudeAdditionalArgs: String
        get() = myState.claudeAdditionalArgs
        set(value) { myState.claudeAdditionalArgs = value }
    
    var claudeFlags: ClaudeFlags
        get() = myState.claudeFlags
        set(value) { myState.claudeFlags = value }
    
    // CliSpecificSettings implementation
    override fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (claudeExecutablePath.isBlank()) {
            errors.add("Claude Code executable path cannot be blank")
        }
        
        if (maxTokens <= 0) {
            errors.add("Max tokens must be positive")
        }
        
        if (temperature < 0.0 || temperature > 2.0) {
            errors.add("Temperature must be between 0.0 and 2.0")
        }
        
        if (topP < 0.0 || topP > 1.0) {
            errors.add("Top-p must be between 0.0 and 1.0")
        }
        
        return errors
    }
    
    override fun getExecutablePath(): String {
        return claudeExecutablePath
    }
    
    override fun getAdditionalArgs(): String {
        return claudeAdditionalArgs
    }
    
    companion object {
        fun getInstance(): ClaudeCodeSpecificSettings {
            return com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(ClaudeCodeSpecificSettings::class.java)
        }
    }
}