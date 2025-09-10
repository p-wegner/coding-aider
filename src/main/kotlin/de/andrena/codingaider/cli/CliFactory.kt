package de.andrena.codingaider.cli

import de.andrena.codingaider.cli.aider.AiderCli
import de.andrena.codingaider.cli.claude.ClaudeCodeCli
import de.andrena.codingaider.settings.GenericCliSettings
import de.andrena.codingaider.settings.SettingsFactory

/**
 * Factory for creating CLI implementations.
 * Manages the lifecycle and selection of different CLI tools.
 */
object CliFactory {
    
    private val cliCache = mutableMapOf<String, CliInterface>()
    
    /**
     * Gets a CLI implementation for the specified CLI tool.
     * @param cliName The name of the CLI tool
     * @return The CLI implementation, or null if not supported
     */
    fun getCli(cliName: String): CliInterface? {
        return when (cliName.lowercase()) {
            "aider" -> cliCache.getOrPut("aider") { AiderCli() }
            "claude", "claude-code" -> cliCache.getOrPut("claude") { ClaudeCodeCli() }
            else -> null
        }
    }
    
    /**
     * Gets the currently selected CLI implementation.
     * @return The currently selected CLI implementation
     */
    fun getCurrentCli(): CliInterface? {
        val settings = GenericCliSettings.getInstance()
        return getCli(settings.selectedCli)
    }
    
    /**
     * Sets the currently selected CLI tool.
     * @param cliName The name of the CLI tool to select
     * @return true if successful, false otherwise
     */
    fun setCurrentCli(cliName: String): Boolean {
        if (isCliSupported(cliName)) {
            val settings = GenericCliSettings.getInstance()
            settings.selectedCli = cliName
            settings.notifySettingsChanged()
            return true
        }
        return false
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
        return getCli(cliName) != null
    }
    
    /**
     * Gets the default CLI tool.
     * @return The default CLI tool name
     */
    fun getDefaultCli(): String {
        return "aider"
    }
    
    /**
     * Gets a CLI implementation with the specified features.
     * @param requiredFeatures Set of required features
     * @return List of CLI tools that support all required features
     */
    fun getCliWithFeatures(requiredFeatures: Set<CliFeature>): List<Pair<String, CliInterface>> {
        return getSupportedCliTools()
            .mapNotNull { cliName -> getCli(cliName)?.let { cliName to it } }
            .filter { (_, cli) -> requiredFeatures.all { feature -> cli.supportsFeature(feature) } }
    }
    
    /**
     * Gets the best CLI tool for a specific feature.
     * @param feature The feature to optimize for
     * @return The best CLI tool name, or null if none support the feature
     */
    fun getBestCliForFeature(feature: CliFeature): String? {
        return getSupportedCliTools()
            .mapNotNull { cliName -> getCli(cliName)?.let { cliName to it } }
            .find { (_, cli) -> cli.supportsFeature(feature) }
            ?.first
    }
    
    /**
     * Gets CLI tool information.
     * @param cliName The name of the CLI tool
     * @return CLI tool information, or null if not supported
     */
    fun getCliInfo(cliName: String): CliInfo? {
        val cli = getCli(cliName) ?: return null
        
        return CliInfo(
            name = cliName,
            executable = cli.getExecutableName(),
            supportedFeatures = CliFeature.values().toSet(),
            argumentMappings = cli.argumentMappings,
            modelHandler = cli.getModelHandler()
        )
    }
    
    /**
     * Gets all CLI tools information.
     * @return List of CLI tool information
     */
    fun getAllCliInfo(): List<CliInfo> {
        return getSupportedCliTools()
            .mapNotNull { getCliInfo(it) }
    }
    
    /**
     * Validates a CLI tool configuration.
     * @param cliName The name of the CLI tool
     * @return List of validation errors, empty if valid
     */
    fun validateCliConfiguration(cliName: String): List<String> {
        val errors = mutableListOf<String>()
        
        val cli = getCli(cliName)
        if (cli == null) {
            errors.add("CLI tool '$cliName' is not supported")
            return errors
        }
        
        // Validate settings
        val settingsErrors = SettingsFactory.validateSettings(cliName)
        errors.addAll(settingsErrors)
        
        // Validate CLI-specific requirements
        val executable = cli.getExecutableName()
        try {
            val process = ProcessBuilder("which", executable).start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                errors.add("Executable '$executable' not found in PATH")
            }
        } catch (e: Exception) {
            errors.add("Failed to check executable '$executable': ${e.message}")
        }
        
        // Validate model handler
        val modelHandler = cli.getModelHandler()
        if (modelHandler.availableModels.isEmpty()) {
            errors.add("No models available for CLI tool '$cliName'")
        }
        
        return errors
    }
    
    /**
     * Clears the CLI cache.
     */
    fun clearCache() {
        cliCache.clear()
    }
    
    /**
     * Refreshes the CLI cache by reloading all CLI implementations.
     */
    fun refreshCache() {
        clearCache()
        // Force reload of current CLI
        getCurrentCli()
    }
    
    /**
     * Gets feature compatibility matrix.
     * @return Map of CLI tools to their supported features
     */
    fun getFeatureCompatibilityMatrix(): Map<String, Set<CliFeature>> {
        return getSupportedCliTools()
            .associateWith { cliName ->
                getCli(cliName)?.let { cli ->
                    CliFeature.values().filter { feature -> cli.supportsFeature(feature) }.toSet()
                } ?: emptySet()
            }
    }
}

/**
 * Data class representing CLI tool information.
 */
data class CliInfo(
    val name: String,
    val executable: String,
    val supportedFeatures: Set<CliFeature>,
    val argumentMappings: Map<GenericArgument, CliArgument>,
    val modelHandler: CliModelHandler
) {
    /**
     * Gets the display name for the CLI tool.
     */
    fun getDisplayName(): String {
        return when (name.lowercase()) {
            "aider" -> "Aider"
            "claude", "claude-code" -> "Claude Code"
            else -> name.replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Gets the description for the CLI tool.
     */
    fun getDescription(): String {
        return when (name.lowercase()) {
            "aider" -> "AI pair programming in your terminal"
            "claude", "claude-code" -> "Claude AI assistant for coding tasks"
            else -> "AI coding assistant"
        }
    }
    
    /**
     * Checks if the CLI tool supports a specific feature.
     */
    fun supportsFeature(feature: CliFeature): Boolean {
        return feature in supportedFeatures
    }
    
    /**
     * Gets the supported features as a formatted string.
     */
    fun getSupportedFeaturesString(): String {
        return supportedFeatures
            .sortedBy { it.name }
            .joinToString(", ") { it.name.replace("_", " ").lowercase() }
    }
}