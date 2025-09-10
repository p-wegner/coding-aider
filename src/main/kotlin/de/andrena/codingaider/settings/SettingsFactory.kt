package de.andrena.codingaider.settings

import com.intellij.openapi.application.ApplicationManager
import de.andrena.codingaider.cli.CliInterface
import de.andrena.codingaider.cli.CliModelHandler

/**
 * Factory for creating and managing CLI-specific settings.
 * This provides a unified interface for accessing settings across different CLI tools.
 */
object SettingsFactory {
    
    /**
     * Gets the appropriate settings service for the given CLI tool.
     * @param cliName The name of the CLI tool
     * @return The settings service, or null if not supported
     */
    fun getCliSettings(cliName: String): CliSpecificSettings? {
        return when (cliName.lowercase()) {
            "aider" -> AiderSpecificSettings.getInstance()
            "claude", "claude-code" -> ClaudeCodeSpecificSettings.getInstance()
            else -> null
        }
    }
    
    /**
     * Gets the generic CLI settings.
     * @return The generic CLI settings
     */
    fun getGenericSettings(): GenericCliSettings {
        return GenericCliSettings.getInstance()
    }
    
    /**
     * Gets all supported CLI tools.
     * @return List of supported CLI tool names
     */
    fun getSupportedCliTools(): List<String> {
        return listOf("aider", "claude")
    }
    
    /**
     * Checks if a CLI tool is supported.
     * @param cliName The name of the CLI tool
     * @return true if supported, false otherwise
     */
    fun isCliSupported(cliName: String): Boolean {
        return getCliSettings(cliName) != null
    }
    
    /**
     * Creates a unified settings view for the given CLI tool.
     * @param cliName The name of the CLI tool
     * @return Unified settings data
     */
    fun createUnifiedSettings(cliName: String): UnifiedSettings {
        val genericSettings = getGenericSettings()
        val cliSettings = getCliSettings(cliName)
        
        return UnifiedSettings(
            selectedCli = cliName,
            genericSettings = genericSettings,
            cliSettings = cliSettings,
            isSupported = cliSettings != null
        )
    }
    
    /**
     * Migrates settings from the old AiderSettings to the new structure.
     * This should be called during plugin upgrade.
     */
    fun migrateLegacySettings() {
        try {
            val legacySettings = LegacyAiderSettingsMigration.migrateLegacySettings()
            if (legacySettings != null) {
                migrateToNewStructure(legacySettings)
            }
        } catch (e: Exception) {
            // Log migration error but don't fail the plugin startup
            System.err.println("Error migrating legacy settings: ${e.message}")
        }
    }
    
    /**
     * Migrates legacy settings to the new structure.
     * @param legacySettings The legacy settings to migrate
     */
    private fun migrateToNewStructure(legacySettings: LegacyAiderSettings) {
        val genericSettings = getGenericSettings()
        val aiderSettings = AiderSpecificSettings.getInstance()
        
        // Migrate generic settings
        genericSettings.defaultModel = legacySettings.llm
        genericSettings.defaultMode = legacySettings.defaultMode.name.lowercase()
        genericSettings.commonExecutionOptions.useDocker = legacySettings.useDockerAider
        genericSettings.commonExecutionOptions.dockerImage = legacySettings.dockerImage
        genericSettings.commonExecutionOptions.useSidecarMode = legacySettings.useSidecarMode
        genericSettings.commonExecutionOptions.sidecarModeVerbose = legacySettings.sidecarModeVerbose
        genericSettings.commonExecutionOptions.additionalArgs = legacySettings.additionalArgs
        genericSettings.commonExecutionOptions.useYesFlag = legacySettings.useYesFlag
        genericSettings.commonExecutionOptions.alwaysIncludeOpenFiles = legacySettings.alwaysIncludeOpenFiles
        genericSettings.commonExecutionOptions.showWorkingDirectoryPanel = legacySettings.showWorkingDirectoryPanel
        
        // Migrate feature flags - disabled for now, can be added later
        // genericSettings.featureFlags.promptAugmentation = legacySettings.promptAugmentation
        
        // Migrate UI preferences - disabled for now, can be added later
        // genericSettings.uiPreferences.verboseCommandLogging = legacySettings.verboseCommandLogging
        
        // Migrate plan settings - disabled for now, can be added later
        // genericSettings.planSettings.planLlm = legacySettings.llm
        // genericSettings.planSettings.enablePlanCompletionLogging = legacySettings.enablePlanCompletionLogging
        
        // Migrate MCP settings
        genericSettings.enableMcpServer = legacySettings.enableMcpServer
        genericSettings.mcpServerPort = legacySettings.mcpServerPort
        genericSettings.mcpServerAutoStart = legacySettings.mcpServerAutoStart
        
        // Migrate Aider-specific settings
        aiderSettings.aiderExecutablePath = legacySettings.aiderExecutablePath
        aiderSettings.reasoningEffort = legacySettings.reasoningEffort
        aiderSettings.editFormat = legacySettings.editFormat
        aiderSettings.lintCmd = legacySettings.lintCmd
        aiderSettings.deactivateRepoMap = legacySettings.deactivateRepoMap
        aiderSettings.includeChangeContext = legacySettings.includeChangeContext
        aiderSettings.autoCommits = legacySettings.autoCommits
        aiderSettings.dirtyCommits = legacySettings.dirtyCommits
        aiderSettings.pluginBasedEdits = legacySettings.pluginBasedEdits
        aiderSettings.lenientEdits = legacySettings.lenientEdits
        aiderSettings.autoCommitAfterEdits = legacySettings.autoCommitAfterEdits
        aiderSettings.defaultAiderMode = legacySettings.defaultMode
        aiderSettings.dockerImage = legacySettings.dockerImage
        aiderSettings.aiderAdditionalArgs = legacySettings.additionalArgs
        
        // Save the migrated settings
        genericSettings.notifySettingsChanged()
    }
    
    /**
     * Validates settings for the given CLI tool.
     * @param cliName The name of the CLI tool
     * @return List of validation errors, empty if valid
     */
    fun validateSettings(cliName: String): List<String> {
        val errors = mutableListOf<String>()
        
        val genericSettings = getGenericSettings()
        val cliSettings = getCliSettings(cliName)
        
        // Validate generic settings
        if (genericSettings.defaultModel.isBlank()) {
            errors.add("Default model cannot be blank")
        }
        
        if (genericSettings.commonExecutionOptions.commandTimeout <= 0) {
            errors.add("Command timeout must be positive")
        }
        
        // Validate CLI-specific settings
        cliSettings?.validate()?.let { cliErrors ->
            errors.addAll(cliErrors)
        }
        
        return errors
    }
}

/**
 * Interface for CLI-specific settings.
 */
interface CliSpecificSettings {
    /**
     * Validates the CLI-specific settings.
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<String>
    
    /**
     * Gets the executable path for the CLI tool.
     * @return The executable path
     */
    fun getExecutablePath(): String
    
    /**
     * Gets the CLI-specific additional arguments.
     * @return The additional arguments
     */
    fun getAdditionalArgs(): String
}

/**
 * Unified settings data class combining generic and CLI-specific settings.
 */
data class UnifiedSettings(
    val selectedCli: String,
    val genericSettings: GenericCliSettings,
    val cliSettings: CliSpecificSettings?,
    val isSupported: Boolean
)

/**
 * Legacy settings migration helper.
 * This handles migration from the old AiderSettings to the new structure.
 */
object LegacyAiderSettingsMigration {
    /**
     * Migrates legacy Aider settings to the new structure.
     * @return The legacy settings, or null if migration is not needed
     */
    fun migrateLegacySettings(): LegacyAiderSettings? {
        // This would typically read from the old AiderSettings.xml file
        // For now, we'll return null to indicate no migration is needed
        // In a real implementation, this would handle the actual migration logic
        return null
    }
}

/**
 * Data class representing legacy Aider settings.
 * This is used for migration purposes only.
 */
data class LegacyAiderSettings(
    val reasoningEffort: String = "",
    val promptAugmentation: Boolean = false,
    val includeCommitMessageBlock: Boolean = false,
    val enableDocumentationLookup: Boolean = false,
    val enableContextYamlExpansion: Boolean = false,
    val enableSubplans: Boolean = false,
    val useSingleFilePlanMode: Boolean = false,
    val useYesFlag: Boolean = false,
    val llm: String = "",
    val additionalArgs: String = "",
    val lintCmd: String = "",
    val showGitComparisonTool: Boolean = false,
    val activateIdeExecutorAfterWebcrawl: Boolean = false,
    val webCrawlLlm: String = "",
    val deactivateRepoMap: Boolean = false,
    val editFormat: String = "",
    val verboseCommandLogging: Boolean = false,
    val useDockerAider: Boolean = false,
    val mountAiderConfInDocker: Boolean = false,
    val includeChangeContext: Boolean = false,
    val autoCommits: AiderSpecificSettings.AutoCommitSetting = AiderSpecificSettings.AutoCommitSetting.DISABLED,
    val dirtyCommits: AiderSpecificSettings.DirtyCommitSetting = AiderSpecificSettings.DirtyCommitSetting.DISABLED,
    val useSidecarMode: Boolean = false,
    val sidecarModeVerbose: Boolean = false,
    val alwaysIncludeOpenFiles: Boolean = false,
    val alwaysIncludePlanContextFiles: Boolean = false,
    val dockerImage: String = "",
    val aiderExecutablePath: String = "",
    val documentationLlm: String = "",
    val planRefinementLlm: String = "",
    val enableAutoPlanContinue: Boolean = false,
    val enableAutoPlanContinuationInPlanFamily: Boolean = false,
    val optionsPanelCollapsed: Boolean = false,
    val enableLocalModelCostMap: Boolean = false,
    val defaultMode: de.andrena.codingaider.inputdialog.AiderMode = de.andrena.codingaider.inputdialog.AiderMode.NORMAL,
    val pluginBasedEdits: Boolean = false,
    val lenientEdits: Boolean = false,
    val autoCommitAfterEdits: Boolean = false,
    val showWorkingDirectoryPanel: Boolean = false,
    val showDevTools: Boolean = false,
    val enableMcpServer: Boolean = false,
    val mcpServerPort: Int = 8080,
    val mcpServerAutoStart: Boolean = false,
    val planCompletionCheckDelay: Int = 500,
    val planCompletionMaxRetries: Int = 3,
    val enablePlanCompletionLogging: Boolean = false
)