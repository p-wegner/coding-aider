package de.andrena.codingaider.settings.cli

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.RoamingType

/**
 * Claude Code specific settings
 */
@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.cli.ClaudeCodeSettings",
    storages = [Storage("ClaudeCodeSettings.xml", roamingType = RoamingType.DISABLED)]
)
class ClaudeCodeSettings : PersistentStateComponent<ClaudeCodeSettings.State>, CliSettings {
    
    data class State(
        var claudeExecutablePath: String = "claude",
        var defaultModel: String = "claude-3-sonnet-20240229",
        var maxTokens: Int = 4000,
        var temperature: Double = 0.7,
        var useDocker: Boolean = false,
        var dockerImage: String = "anthropic/claude-code:latest",
        var additionalArgs: String = "",
        var verboseLogging: Boolean = false,
        var showWorkingDirectoryPanel: Boolean = true,
        var showDevTools: Boolean = false,
        var enableMcpServer: Boolean = false,
        var mcpServerPort: Int = 8081,
        var mcpServerAutoStart: Boolean = false
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    override fun getCliType(): CliType = CliType.CLAUDE_CODE
    
    override fun getExecutableName(): String = myState.claudeExecutablePath
    
    override fun getDisplayName(): String = "Claude Code"
    
    override fun getPromptArgument(): String = "-p"
    
    override fun getDefaultModel(): String = myState.defaultModel
    
    override fun supportsFeature(feature: CliFeature): Boolean = when (feature) {
        CliFeature.AUTO_COMMIT -> false // Claude Code doesn't support auto-commits
        CliFeature.DIRTY_COMMIT -> false // Claude Code doesn't support dirty commits
        CliFeature.EDIT_FORMAT -> false // Claude Code has its own edit format
        CliFeature.LINT_COMMAND -> false // Claude Code doesn't have a lint command
        CliFeature.PLUGIN_BASED_EDITS -> false // Claude Code doesn't support plugin-based edits
        CliFeature.SIDEKAR_MODE -> false // Claude Code doesn't support sidecar mode
        CliFeature.DOCKER_SUPPORT -> true
        CliFeature.PLANS_SUPPORT -> true // Claude Code supports plan-based development
        CliFeature.MCP_SERVER -> true // Claude Code can integrate with MCP
        CliFeature.REASONING_EFFORT -> false // Claude Code doesn't have reasoning effort setting
        CliFeature.CONTEXT_YAML_EXPANSION -> false // Claude Code doesn't support context YAML expansion
    }
    
    override fun validateConfiguration(): CliValidationResult {
        val errors = mutableListOf<String>()
        
        if (myState.defaultModel.isBlank()) {
            errors.add("Model cannot be empty")
        }
        
        if (myState.maxTokens <= 0) {
            errors.add("Max tokens must be greater than 0")
        }
        
        if (myState.temperature < 0.0 || myState.temperature > 2.0) {
            errors.add("Temperature must be between 0.0 and 2.0")
        }
        
        if (myState.mcpServerPort <= 0 || myState.mcpServerPort > 65535) {
            errors.add("MCP server port must be between 1 and 65535")
        }
        
        return CliValidationResult(
            isValid = errors.isEmpty(),
            errorMessages = errors
        )
    }
    
    // Property accessors
    var claudeExecutablePath: String
        get() = myState.claudeExecutablePath
        set(value) { myState.claudeExecutablePath = value }
    
    var model: String
        get() = myState.defaultModel
        set(value) { myState.defaultModel = value }
    
    var maxTokens: Int
        get() = myState.maxTokens
        set(value) { myState.maxTokens = value }
    
    var temperature: Double
        get() = myState.temperature
        set(value) { myState.temperature = value }
    
    var useDocker: Boolean
        get() = myState.useDocker
        set(value) { myState.useDocker = value }
    
    var dockerImage: String
        get() = myState.dockerImage
        set(value) { myState.dockerImage = value }
    
    var additionalArgs: String
        get() = myState.additionalArgs
        set(value) { myState.additionalArgs = value }
    
    var verboseLogging: Boolean
        get() = myState.verboseLogging
        set(value) { myState.verboseLogging = value }
    
    var showWorkingDirectoryPanel: Boolean
        get() = myState.showWorkingDirectoryPanel
        set(value) { myState.showWorkingDirectoryPanel = value }
    
    var showDevTools: Boolean
        get() = myState.showDevTools
        set(value) { myState.showDevTools = value }
    
    var enableMcpServer: Boolean
        get() = myState.enableMcpServer
        set(value) { myState.enableMcpServer = value }
    
    var mcpServerPort: Int
        get() = myState.mcpServerPort
        set(value) { myState.mcpServerPort = value }
    
    var mcpServerAutoStart: Boolean
        get() = myState.mcpServerAutoStart
        set(value) { myState.mcpServerAutoStart = value }
    
    companion object {
        fun getInstance(): ClaudeCodeSettings =
            com.intellij.openapi.application.ApplicationManager.getApplication().getService(ClaudeCodeSettings::class.java)
    }
}