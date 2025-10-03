package de.andrena.codingaider.settings.cli

import de.andrena.codingaider.inputdialog.AiderMode

/**
 * Generic CLI settings interface that all CLI implementations should extend
 */
interface CliSettings {
    /**
     * Get the CLI type identifier
     */
    fun getCliType(): CliType
    
    /**
     * Get the CLI executable name
     */
    fun getExecutableName(): String
    
    /**
     * Get the CLI display name
     */
    fun getDisplayName(): String
    
    /**
     * Get the CLI prompt argument (e.g., "-m" for Aider, "-p" for Claude Code)
     */
    fun getPromptArgument(): String
    
    /**
     * Get the default model for this CLI
     */
    fun getDefaultModel(): String
    
    /**
     * Check if this CLI supports a specific feature
     */
    fun supportsFeature(feature: CliFeature): Boolean
    
    /**
     * Validate configuration for this CLI
     */
    fun validateConfiguration(): CliValidationResult
}

/**
 * Supported CLI types
 */
enum class CliType {
    AIDER,
    CLAUDE_CODE,
    GEMINI_CLI,
    CODEX_CLI
}

/**
 * Features that different CLIs may support
 */
enum class CliFeature {
    AUTO_COMMIT,
    DIRTY_COMMIT,
    EDIT_FORMAT,
    LINT_COMMAND,
    PLUGIN_BASED_EDITS,
    SIDEKAR_MODE,
    DOCKER_SUPPORT,
    PLANS_SUPPORT,
    MCP_SERVER,
    REASONING_EFFORT,
    CONTEXT_YAML_EXPANSION
}

/**
 * Result of CLI configuration validation
 */
data class CliValidationResult(
    val isValid: Boolean,
    val errorMessages: List<String> = emptyList()
)

/**
 * Common execution options that apply to all CLIs
 */
data class CommonExecutionOptions(
    val useDocker: Boolean = false,
    val dockerImage: String = "",
    val verboseLogging: Boolean = false,
    val additionalArgs: String = "",
    val showWorkingDirectoryPanel: Boolean = true,
    val showDevTools: Boolean = false
)

/**
 * Common LLM provider configuration
 */
data class CommonLlmProviderConfig(
    val providerType: String,
    val modelName: String,
    val apiKey: String = "",
    val baseUrl: String = "",
    val maxTokens: Int = 4000,
    val temperature: Double = 0.7
)