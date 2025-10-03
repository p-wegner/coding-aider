package de.andrena.codingaider.settings.cli

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.RoamingType
import de.andrena.codingaider.settings.AiderSettings

/**
 * Generic CLI settings service that manages the selected CLI and provides access to all CLI settings
 */
@Service(Service.Level.APP)
@State(
    name = "de.andrena.codingaider.settings.cli.GenericCliSettings",
    storages = [Storage("GenericCliSettings.xml", roamingType = RoamingType.DISABLED)]
)
class GenericCliSettings : PersistentStateComponent<GenericCliSettings.State> {
    
    data class State(
        var selectedCli: CliType = CliType.AIDER,
        var commonExecutionOptions: CommonExecutionOptions = CommonExecutionOptions()
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    /**
     * Get the currently selected CLI type
     */
    fun getSelectedCli(): CliType = myState.selectedCli
    
    /**
     * Set the currently selected CLI type
     */
    fun setSelectedCli(cliType: CliType) {
        myState.selectedCli = cliType
    }
    
    /**
     * Get the CLI settings for the currently selected CLI
     */
    fun getCurrentCliSettings(): CliSettings = when (myState.selectedCli) {
        CliType.AIDER -> getAiderCliSettings()
        CliType.CLAUDE_CODE -> ClaudeCodeSettings.getInstance()
        CliType.GEMINI_CLI -> throw UnsupportedOperationException("Gemini CLI not yet implemented")
        CliType.CODEX_CLI -> throw UnsupportedOperationException("Codex CLI not yet implemented")
    }
    
    /**
     * Get Aider CLI settings (wraps existing AiderSettings)
     */
    private fun getAiderCliSettings(): CliSettings = object : CliSettings {
        private val aiderSettings = AiderSettings.getInstance()
        
        override fun getCliType(): CliType = CliType.AIDER
        override fun getExecutableName(): String = aiderSettings.aiderExecutablePath
        override fun getDisplayName(): String = "Aider"
        override fun getPromptArgument(): String = "-m"
        override fun getDefaultModel(): String = aiderSettings.llm
        override fun supportsFeature(feature: CliFeature): Boolean = when (feature) {
            CliFeature.AUTO_COMMIT -> true
            CliFeature.DIRTY_COMMIT -> true
            CliFeature.EDIT_FORMAT -> true
            CliFeature.LINT_COMMAND -> true
            CliFeature.PLUGIN_BASED_EDITS -> true
            CliFeature.SIDEKAR_MODE -> true
            CliFeature.DOCKER_SUPPORT -> true
            CliFeature.PLANS_SUPPORT -> true
            CliFeature.MCP_SERVER -> true
            CliFeature.REASONING_EFFORT -> true
            CliFeature.CONTEXT_YAML_EXPANSION -> true
        }
        override fun validateConfiguration(): CliValidationResult = CliValidationResult(isValid = true)
    }
    
    /**
     * Get common execution options
     */
    fun getCommonExecutionOptions(): CommonExecutionOptions = myState.commonExecutionOptions
    
    /**
     * Update common execution options
     */
    fun updateCommonExecutionOptions(options: CommonExecutionOptions) {
        myState.commonExecutionOptions = options
    }
    
    companion object {
        fun getInstance(): GenericCliSettings =
            ApplicationManager.getApplication().getService(GenericCliSettings::class.java)
    }
}