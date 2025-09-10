package de.andrena.codingaider.cli.aider

import de.andrena.codingaider.cli.BaseCliArgumentMapper
import de.andrena.codingaider.cli.CliArgument
import de.andrena.codingaider.cli.GenericArgument

/**
 * Argument mapper for Aider CLI.
 * Handles mapping between generic arguments and Aider-specific arguments.
 */
class AiderArgumentMapper : BaseCliArgumentMapper() {
    
    override val argumentMappings: Map<GenericArgument, CliArgument> = mapOf(
        GenericArgument.PROMPT to CliArgument.value("-m", "Main prompt/message for Aider"),
        GenericArgument.MODEL to CliArgument.value("--model", "Language model to use"),
        GenericArgument.FILE to CliArgument.value("--file", "File to include in context"),
        GenericArgument.READ_ONLY_FILE to CliArgument.value("--read", "Read-only file to include in context"),
        GenericArgument.YES_FLAG to CliArgument.flag("--yes", "Automatically confirm all prompts"),
        GenericArgument.EDIT_FORMAT to CliArgument.value("--edit-format", "Format for edit instructions"),
        GenericArgument.LINT_COMMAND to CliArgument.value("--lint-cmd", "Command to run for linting"),
        GenericArgument.AUTO_COMMIT to CliArgument.flag("--auto-commits", "Automatically commit changes"),
        GenericArgument.DIRTY_COMMIT to CliArgument.flag("--dirty-commits", "Commit changes before execution"),
        GenericArgument.REASONING_EFFORT to CliArgument.value("--reasoning-effort", "Reasoning effort level"),
        GenericArgument.DEACTIVATE_REPO_MAP to CliArgument.flag("--no-repo-map", "Disable repository mapping"),
        GenericArgument.INCLUDE_CHANGE_CONTEXT to CliArgument.flag("--include-change-context", "Include change context"),
        GenericArgument.SHELL_MODE to CliArgument.flag("--shell", "Enable shell mode"),
        GenericArgument.STRUCTURED_MODE to CliArgument.flag("--architect", "Enable architect mode"),
        GenericArgument.VERBOSE to CliArgument.flag("--verbose", "Enable verbose output"),
        GenericArgument.QUIET to CliArgument.flag("--no-pretty", "Disable pretty output"),
        GenericArgument.CONFIG_FILE to CliArgument.value("--config", "Configuration file path"),
        GenericArgument.CACHE_DIRECTORY to CliArgument.value("--cache-dir", "Cache directory path"),
        GenericArgument.LOG_FILE to CliArgument.value("--log-file", "Log file path"),
        GenericArgument.TIMEOUT to CliArgument.value("--timeout", "Timeout duration"),
        GenericArgument.API_KEY to CliArgument.value("--api-key", "API key for authentication"),
        GenericArgument.API_BASE_URL to CliArgument.value("--api-base", "API base URL"),
        GenericArgument.PROXY to CliArgument.value("--proxy", "Proxy configuration"),
        GenericArgument.DEBUG to CliArgument.flag("--debug", "Enable debug mode"),
        GenericArgument.DRY_RUN to CliArgument.flag("--dry-run", "Enable dry run mode"),
        GenericArgument.HELP to CliArgument.flag("--help", "Show help information"),
        GenericArgument.VERSION to CliArgument.flag("--version", "Show version information")
    )
    
    override fun getPromptArgument(): CliArgument {
        return CliArgument.value("-m", "Main prompt/message for Aider")
    }
    
    override fun getFileArgument(isReadOnly: Boolean): CliArgument {
        return if (isReadOnly) {
            CliArgument.value("--read", "Read-only file to include in context")
        } else {
            CliArgument.value("--file", "File to include in context")
        }
    }
    
    override fun getModelArgument(): CliArgument {
        return CliArgument.value("--model", "Language model to use")
    }
    
    override fun getYesFlagArgument(): CliArgument? {
        return CliArgument.flag("--yes", "Automatically confirm all prompts")
    }
    
    override fun getEditFormatArgument(): CliArgument? {
        return CliArgument.value("--edit-format", "Format for edit instructions")
    }
    
    override fun getLintCommandArgument(): CliArgument? {
        return CliArgument.value("--lint-cmd", "Command to run for linting")
    }
    
    override fun getAutoCommitArgument(enabled: Boolean): CliArgument? {
        return if (enabled) {
            CliArgument.flag("--auto-commits", "Automatically commit changes")
        } else {
            null
        }
    }
    
    override fun getDirtyCommitArgument(enabled: Boolean): CliArgument? {
        return if (enabled) {
            CliArgument.flag("--dirty-commits", "Commit changes before execution")
        } else {
            null
        }
    }
    
    override fun orderArguments(genericArgs: Map<GenericArgument, String>): Map<GenericArgument, String> {
        // Order arguments according to Aider's preferred order
        val orderedArgs = linkedMapOf<GenericArgument, String>()
        
        // Model should come first
        genericArgs[GenericArgument.MODEL]?.let { orderedArgs[GenericArgument.MODEL] = it }
        
        // Prompt should come early
        genericArgs[GenericArgument.PROMPT]?.let { orderedArgs[GenericArgument.PROMPT] = it }
        
        // Files come next
        genericArgs.filterKeys { it == GenericArgument.FILE || it == GenericArgument.READ_ONLY_FILE }
            .forEach { (key, value) -> orderedArgs[key] = value }
        
        // Configuration options
        genericArgs[GenericArgument.CONFIG_FILE]?.let { orderedArgs[GenericArgument.CONFIG_FILE] = it }
        genericArgs[GenericArgument.EDIT_FORMAT]?.let { orderedArgs[GenericArgument.EDIT_FORMAT] = it }
        genericArgs[GenericArgument.LINT_COMMAND]?.let { orderedArgs[GenericArgument.LINT_COMMAND] = it }
        genericArgs[GenericArgument.REASONING_EFFORT]?.let { orderedArgs[GenericArgument.REASONING_EFFORT] = it }
        
        // Feature flags
        genericArgs[GenericArgument.YES_FLAG]?.let { orderedArgs[GenericArgument.YES_FLAG] = it }
        genericArgs[GenericArgument.AUTO_COMMIT]?.let { orderedArgs[GenericArgument.AUTO_COMMIT] = it }
        genericArgs[GenericArgument.DIRTY_COMMIT]?.let { orderedArgs[GenericArgument.DIRTY_COMMIT] = it }
        genericArgs[GenericArgument.DEACTIVATE_REPO_MAP]?.let { orderedArgs[GenericArgument.DEACTIVATE_REPO_MAP] = it }
        genericArgs[GenericArgument.INCLUDE_CHANGE_CONTEXT]?.let { orderedArgs[GenericArgument.INCLUDE_CHANGE_CONTEXT] = it }
        genericArgs[GenericArgument.SHELL_MODE]?.let { orderedArgs[GenericArgument.SHELL_MODE] = it }
        genericArgs[GenericArgument.STRUCTURED_MODE]?.let { orderedArgs[GenericArgument.STRUCTURED_MODE] = it }
        
        // Output and debugging options
        genericArgs[GenericArgument.VERBOSE]?.let { orderedArgs[GenericArgument.VERBOSE] = it }
        genericArgs[GenericArgument.QUIET]?.let { orderedArgs[GenericArgument.QUIET] = it }
        genericArgs[GenericArgument.DEBUG]?.let { orderedArgs[GenericArgument.DEBUG] = it }
        genericArgs[GenericArgument.LOG_FILE]?.let { orderedArgs[GenericArgument.LOG_FILE] = it }
        
        // Execution options
        genericArgs[GenericArgument.TIMEOUT]?.let { orderedArgs[GenericArgument.TIMEOUT] = it }
        genericArgs[GenericArgument.DRY_RUN]?.let { orderedArgs[GenericArgument.DRY_RUN] = it }
        
        // API configuration
        genericArgs[GenericArgument.API_KEY]?.let { orderedArgs[GenericArgument.API_KEY] = it }
        genericArgs[GenericArgument.API_BASE_URL]?.let { orderedArgs[GenericArgument.API_BASE_URL] = it }
        genericArgs[GenericArgument.PROXY]?.let { orderedArgs[GenericArgument.PROXY] = it }
        
        // Cache and directories
        genericArgs[GenericArgument.CACHE_DIRECTORY]?.let { orderedArgs[GenericArgument.CACHE_DIRECTORY] = it }
        
        // Help and version
        genericArgs[GenericArgument.HELP]?.let { orderedArgs[GenericArgument.HELP] = it }
        genericArgs[GenericArgument.VERSION]?.let { orderedArgs[GenericArgument.VERSION] = it }
        
        // Add any remaining arguments
        genericArgs.filterKeys { !orderedArgs.containsKey(it) }
            .forEach { (key, value) -> orderedArgs[key] = value }
        
        return orderedArgs
    }
    
    override fun validateArguments(genericArgs: Map<GenericArgument, String>): List<String> {
        val errors: MutableList<String> = super.validateArguments(genericArgs).toMutableList()
        
        // Aider-specific validation
        val hasModel = genericArgs.containsKey(GenericArgument.MODEL)
        val hasPrompt = genericArgs.containsKey(GenericArgument.PROMPT)
        
        if (!hasModel) {
            errors.add("Model argument is required for Aider")
        }
        
        if (!hasPrompt) {
            errors.add("Prompt argument is required for Aider")
        }
        
        // Validate mutually exclusive modes
        val hasShellMode = genericArgs[GenericArgument.SHELL_MODE] == "true"
        val hasStructuredMode = genericArgs[GenericArgument.STRUCTURED_MODE] == "true"
        
        if (hasShellMode && hasStructuredMode) {
            errors.add("Shell mode and structured mode are mutually exclusive")
        }
        
        // Validate reasoning effort format
        genericArgs[GenericArgument.REASONING_EFFORT]?.let { reasoningEffort ->
            val validEfforts = listOf("none", "low", "medium", "high")
            if (reasoningEffort.lowercase() !in validEfforts) {
                errors.add("Reasoning effort must be one of: $validEfforts")
            }
        }
        
        // Validate edit format
        genericArgs[GenericArgument.EDIT_FORMAT]?.let { editFormat ->
            val validFormats = listOf("diff", "whole", "udiff")
            if (editFormat.lowercase() !in validFormats) {
                errors.add("Edit format must be one of: $validFormats")
            }
        }
        
        return errors
    }
    
    /**
     * Gets the default arguments that should always be included for Aider.
     * @return List of default arguments
     */
    fun getDefaultArguments(): List<String> {
        return listOf(
            "--no-suggest-shell-commands",
            "--no-pretty"
        )
    }
    
    /**
     * Gets the arguments for a specific Aider mode.
     * @param mode The Aider mode
     * @return List of mode-specific arguments
     */
    fun getModeArguments(mode: String): List<String> {
        return when (mode.lowercase()) {
            "architect", "structured" -> listOf("--architect")
            "shell" -> listOf("--shell")
            else -> emptyList()
        }
    }
    
    /**
     * Gets the Docker-specific arguments for Aider.
     * @param dockerImage The Docker image to use
     * @param mountConfig Whether to mount configuration
     * @return List of Docker-specific arguments
     */
    fun getDockerArguments(dockerImage: String, mountConfig: Boolean): List<String> {
        val args = mutableListOf<String>()
        
        if (dockerImage.isNotBlank()) {
            args.add("--docker")
            args.add(dockerImage)
        }
        
        if (mountConfig) {
            args.add("--mount-config")
        }
        
        return args
    }
    
    /**
     * Gets the sidecar mode arguments for Aider.
     * @param verbose Whether to enable verbose logging
     * @return List of sidecar-specific arguments
     */
    fun getSidecarArguments(verbose: Boolean): List<String> {
        val args = mutableListOf<String>()
        
        args.add("--sidecar")
        
        if (verbose) {
            args.add("--verbose")
        }
        
        return args
    }
}