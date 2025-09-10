package de.andrena.codingaider.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.messages.Topic
import de.andrena.codingaider.cli.CliMode

/**
 * Generic CLI settings that are common across different AI coding assistants.
 * This provides the foundation for CLI-agnostic configuration.
 */
@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.GenericCliSettings",
    storages = [Storage("GenericCliSettings.xml", roamingType = RoamingType.DISABLED)]
)
class GenericCliSettings : PersistentStateComponent<GenericCliSettings.State> {
    
    private val settingsChangeListeners = mutableListOf<SettingsChangeListener>()
    
    fun addSettingsChangeListener(listener: SettingsChangeListener) {
        settingsChangeListeners.add(listener)
    }
    
    fun removeSettingsChangeListener(listener: SettingsChangeListener) {
        settingsChangeListeners.remove(listener)
    }
    
    fun notifySettingsChanged() {
        settingsChangeListeners.forEach { it.onSettingsChanged() }
        
        // Update tool windows when settings change
        ApplicationManager.getApplication().invokeLater {
            com.intellij.openapi.project.ProjectManager.getInstance().openProjects.forEach { project ->
                val toolWindowManager = ToolWindowManager.getInstance(project)
                // Update relevant tool windows based on settings
                updateToolWindows(toolWindowManager, project)
            }
        }
    }
    
    private fun updateToolWindows(toolWindowManager: ToolWindowManager, project: com.intellij.openapi.project.Project) {
        // Update MCP Server tool window if it exists
        val mcpToolWindow = toolWindowManager.getToolWindow("MCP Server")
        if (mcpToolWindow != null) {
            mcpToolWindow.setAvailable(myState.enableMcpServer, null)
        }
    }
    
    data class State(
        /**
         * Currently selected CLI tool
         */
        var selectedCli: String = "aider",
        
        /**
         * Default model to use
         */
        var defaultModel: String = "gpt-4",
        
        /**
         * Default execution mode
         */
        var defaultMode: String = CliMode.NORMAL.cliIdentifier,
        
        /**
         * Common execution options
         */
        var commonExecutionOptions: CommonExecutionOptions = CommonExecutionOptions(),
        
        /**
         * LLM provider configurations
         */
        var llmProviders: List<LlmProviderConfig> = emptyList(),
        
        /**
         * Feature flags
         */
        var featureFlags: FeatureFlags = FeatureFlags(),
        
        /**
         * UI preferences
         */
        var uiPreferences: UiPreferences = UiPreferences(),
        
        /**
         * Plan settings
         */
        var planSettings: PlanSettings = PlanSettings(),
        
        /**
         * MCP Server settings
         */
        var enableMcpServer: Boolean = false,
        var mcpServerPort: Int = 8080,
        var mcpServerAutoStart: Boolean = false,
        
        /**
         * Development settings
         */
        var developmentSettings: DevelopmentSettings = DevelopmentSettings()
    )
    
    data class CommonExecutionOptions(
        /**
         * Whether to use Docker for execution
         */
        var useDocker: Boolean = false,
        
        /**
         * Docker image to use
         */
        var dockerImage: String = "",
        
        /**
         * Whether to mount configuration in Docker
         */
        var mountConfigInDocker: Boolean = false,
        
        /**
         * Whether to use sidecar mode
         */
        var useSidecarMode: Boolean = false,
        
        /**
         * Verbose logging for sidecar mode
         */
        var sidecarModeVerbose: Boolean = false,
        
        /**
         * Additional command line arguments
         */
        var additionalArgs: String = "",
        
        /**
         * Working directory
         */
        var workingDirectory: String = "",
        
        /**
         * Timeout for command execution (in milliseconds)
         */
        var commandTimeout: Long = 300000,
        
        /**
         * Whether to automatically confirm prompts
         */
        var useYesFlag: Boolean = false,
        
        /**
         * Whether to disable presentation of changes
         */
        var disablePresentation: Boolean = false,
        
        /**
         * Whether to always include open files
         */
        var alwaysIncludeOpenFiles: Boolean = false,
        
        /**
         * Whether to show working directory panel
         */
        var showWorkingDirectoryPanel: Boolean = true
    )
    
    data class LlmProviderConfig(
        /**
         * Provider name (e.g., "openai", "anthropic")
         */
        var name: String,
        
        /**
         * API key
         */
        var apiKey: String = "",
        
        /**
         * Base URL
         */
        var baseUrl: String = "",
        
        /**
         * Default model for this provider
         */
        var defaultModel: String = "",
        
        /**
         * Whether this provider is enabled
         */
        var enabled: Boolean = true,
        
        /**
         * Provider-specific settings
         */
        var providerSettings: Map<String, String> = emptyMap()
    )
    
    data class FeatureFlags(
        /**
         * Whether to enable prompt augmentation
         */
        var promptAugmentation: Boolean = true,
        
        /**
         * Whether to enable documentation lookup
         */
        var enableDocumentationLookup: Boolean = true,
        
        /**
         * Whether to enable context YAML expansion
         */
        var enableContextYamlExpansion: Boolean = true,
        
        /**
         * Whether to enable subplans
         */
        var enableSubplans: Boolean = true,
        
        /**
         * Whether to use single file plan mode
         */
        var useSingleFilePlanMode: Boolean = false,
        
        /**
         * Whether to enable local model cost map
         */
        var enableLocalModelCostMap: Boolean = false,
        
        /**
         * Whether to enable auto plan continue
         */
        var enableAutoPlanContinue: Boolean = false,
        
        /**
         * Whether to enable auto plan continuation in plan family
         */
        var enableAutoPlanContinuationInPlanFamily: Boolean = false,
        
        /**
         * Whether to include commit message block
         */
        var includeCommitMessageBlock: Boolean = false
    )
    
    data class UiPreferences(
        /**
         * Whether to collapse options panel by default
         */
        var optionsPanelCollapsed: Boolean = true,
        
        /**
         * Whether to show dev tools
         */
        var showDevTools: Boolean = false,
        
        /**
         * Whether to show git comparison tool
         */
        var showGitComparisonTool: Boolean = true,
        
        /**
         * Whether to activate IDE executor after web crawl
         */
        var activateIdeExecutorAfterWebcrawl: Boolean = true,
        
        /**
         * Whether to use verbose command logging
         */
        var verboseCommandLogging: Boolean = false
    )
    
    data class PlanSettings(
        /**
         * Default LLM for plan operations
         */
        var planLlm: String = "gpt-4",
        
        /**
         * LLM for web crawling
         */
        var webCrawlLlm: String = "gpt-4",
        
        /**
         * LLM for documentation
         */
        var documentationLlm: String = "gpt-4",
        
        /**
         * LLM for plan refinement
         */
        var planRefinementLlm: String = "gpt-4",
        
        /**
         * Whether to always include plan context files
         */
        var alwaysIncludePlanContextFiles: Boolean = true,
        
        /**
         * Plan completion check delay (milliseconds)
         */
        var planCompletionCheckDelay: Int = 500,
        
        /**
         * Plan completion max retries
         */
        var planCompletionMaxRetries: Int = 3,
        
        /**
         * Whether to enable plan completion logging
         */
        var enablePlanCompletionLogging: Boolean = false
    )
    
    data class DevelopmentSettings(
        /**
         * Whether to enable development mode
         */
        var developmentMode: Boolean = false,
        
        /**
         * Development features
         */
        var developmentFeatures: Map<String, Boolean> = emptyMap()
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    // Property delegates for backward compatibility
    var selectedCli: String
        get() = myState.selectedCli
        set(value) {
            myState.selectedCli = value
            notifySettingsChanged()
        }
    
    var defaultModel: String
        get() = myState.defaultModel
        set(value) {
            myState.defaultModel = value
            notifySettingsChanged()
        }
    
    var defaultMode: String
        get() = myState.defaultMode
        set(value) {
            myState.defaultMode = value
            notifySettingsChanged()
        }
    
    var commonExecutionOptions: CommonExecutionOptions
        get() = myState.commonExecutionOptions
        set(value) {
            myState.commonExecutionOptions = value
            notifySettingsChanged()
        }
    
    var enableMcpServer: Boolean
        get() = myState.enableMcpServer
        set(value) {
            myState.enableMcpServer = value
            notifySettingsChanged()
        }
    
    var mcpServerPort: Int
        get() = myState.mcpServerPort
        set(value) {
            myState.mcpServerPort = value
            notifySettingsChanged()
        }
    
    var mcpServerAutoStart: Boolean
        get() = myState.mcpServerAutoStart
        set(value) {
            myState.mcpServerAutoStart = value
            notifySettingsChanged()
        }
    
    companion object {
        val SETTINGS_CHANGED_TOPIC = Topic("GenericCliSettingsChanged", SettingsChangeListener::class.java)
        
        fun getInstance(): GenericCliSettings {
            return ApplicationManager.getApplication().getService(GenericCliSettings::class.java)
        }
    }
    
    interface SettingsChangeListener {
        fun onSettingsChanged()
    }
}