package de.andrena.codingaider.cli.claude

import de.andrena.codingaider.cli.BaseCliArgumentMapper
import de.andrena.codingaider.cli.CliArgument
import de.andrena.codingaider.cli.GenericArgument

/**
 * Argument mapper for Claude Code CLI.
 * Handles mapping between generic arguments and Claude Code-specific arguments.
 */
class ClaudeCodeArgumentMapper : BaseCliArgumentMapper() {
    
    override val argumentMappings: Map<GenericArgument, CliArgument> = mapOf<GenericArgument, CliArgument>(
        GenericArgument.PROMPT to CliArgument.value("-p", "Main prompt/question for Claude"),
        GenericArgument.MODEL to CliArgument.value("--model", "Model to use for generation"),
        GenericArgument.FILE to CliArgument.value("--file", "File to include in context"),
        GenericArgument.READ_ONLY_FILE to CliArgument.value("--read", "Read-only file to include in context"),
        GenericArgument.MAX_TOKENS to CliArgument.value("--max-tokens", "Maximum tokens for generation"),
        GenericArgument.TEMPERATURE to CliArgument.value("--temperature", "Temperature parameter"),
        GenericArgument.TOP_P to CliArgument.value("--top-p", "Top-p parameter"),
        GenericArgument.SYSTEM_PROMPT to CliArgument.value("--system", "System prompt"),
        GenericArgument.USER_CONTEXT to CliArgument.value("--context", "User context"),
        GenericArgument.VERBOSE to CliArgument.flag("--verbose", "Enable verbose output"),
        GenericArgument.QUIET to CliArgument.flag("--quiet", "Enable quiet mode"),
        GenericArgument.STREAMING_RESPONSES to CliArgument.flag("--stream", "Enable streaming responses"),
        GenericArgument.DEBUG to CliArgument.flag("--debug", "Enable debug mode"),
        GenericArgument.CONFIG_FILE to CliArgument.value("--config", "Configuration file path"),
        GenericArgument.API_KEY to CliArgument.value("--api-key", "API key for authentication"),
        GenericArgument.API_BASE_URL to CliArgument.value("--api-base", "API base URL"),
        GenericArgument.TIMEOUT to CliArgument.value("--timeout", "Timeout duration"),
        GenericArgument.HELP to CliArgument.flag("--help", "Show help information"),
        GenericArgument.VERSION to CliArgument.flag("--version", "Show version information")
    )
    
    override fun getPromptArgument(): CliArgument {
        return CliArgument.value("-p", "Main prompt/question for Claude")
    }
    
    override fun getFileArgument(isReadOnly: Boolean): CliArgument {
        return if (isReadOnly) {
            CliArgument.value("--read", "Read-only file to include in context")
        } else {
            CliArgument.value("--file", "File to include in context")
        }
    }
    
    override fun getModelArgument(): CliArgument {
        return CliArgument.value("--model", "Model to use for generation")
    }
    
    override fun getYesFlagArgument(): CliArgument? {
        // Claude Code doesn't have a yes flag
        return null
    }
    
    override fun getEditFormatArgument(): CliArgument? {
        // Claude Code doesn't have edit format argument
        return null
    }
    
    override fun getLintCommandArgument(): CliArgument? {
        // Claude Code doesn't have lint command argument
        return null
    }
    
    override fun getAutoCommitArgument(enabled: Boolean): CliArgument? {
        // Claude Code doesn't have auto-commit argument
        return null
    }
    
    override fun getDirtyCommitArgument(enabled: Boolean): CliArgument? {
        // Claude Code doesn't have dirty-commit argument
        return null
    }
    
    override fun orderArguments(genericArgs: Map<GenericArgument, String>): Map<GenericArgument, String> {
        // Order arguments according to Claude Code's preferred order
        val orderedArgs = linkedMapOf<GenericArgument, String>()
        
        // Model should come first
        genericArgs[GenericArgument.MODEL]?.let { orderedArgs[GenericArgument.MODEL] = it }
        
        // Prompt should come early
        genericArgs[GenericArgument.PROMPT]?.let { orderedArgs[GenericArgument.PROMPT] = it }
        
        // Generation parameters
        genericArgs[GenericArgument.MAX_TOKENS]?.let { orderedArgs[GenericArgument.MAX_TOKENS] = it }
        genericArgs[GenericArgument.TEMPERATURE]?.let { orderedArgs[GenericArgument.TEMPERATURE] = it }
        genericArgs[GenericArgument.TOP_P]?.let { orderedArgs[GenericArgument.TOP_P] = it }
        
        // System prompt and context
        genericArgs[GenericArgument.SYSTEM_PROMPT]?.let { orderedArgs[GenericArgument.SYSTEM_PROMPT] = it }
        genericArgs[GenericArgument.USER_CONTEXT]?.let { orderedArgs[GenericArgument.USER_CONTEXT] = it }
        
        // Files come next
        genericArgs.filterKeys { it == GenericArgument.FILE || it == GenericArgument.READ_ONLY_FILE }
            .forEach { (key, value) -> orderedArgs[key] = value }
        
        // Configuration options
        genericArgs[GenericArgument.CONFIG_FILE]?.let { orderedArgs[GenericArgument.CONFIG_FILE] = it }
        
        // Feature flags
        genericArgs[GenericArgument.STREAMING_RESPONSES]?.let { orderedArgs[GenericArgument.STREAMING_RESPONSES] = it }
        genericArgs[GenericArgument.VERBOSE]?.let { orderedArgs[GenericArgument.VERBOSE] = it }
        genericArgs[GenericArgument.QUIET]?.let { orderedArgs[GenericArgument.QUIET] = it }
        genericArgs[GenericArgument.DEBUG]?.let { orderedArgs[GenericArgument.DEBUG] = it }
        
        // Execution options
        genericArgs[GenericArgument.TIMEOUT]?.let { orderedArgs[GenericArgument.TIMEOUT] = it }
        
        // API configuration
        genericArgs[GenericArgument.API_KEY]?.let { orderedArgs[GenericArgument.API_KEY] = it }
        genericArgs[GenericArgument.API_BASE_URL]?.let { orderedArgs[GenericArgument.API_BASE_URL] = it }
        
        // Help and version
        genericArgs[GenericArgument.HELP]?.let { orderedArgs[GenericArgument.HELP] = it }
        genericArgs[GenericArgument.VERSION]?.let { orderedArgs[GenericArgument.VERSION] = it }
        
        // Add any remaining arguments
        genericArgs.filterKeys { !orderedArgs.containsKey(it) }
            .forEach { (key, value) -> orderedArgs[key] = value }
        
        return orderedArgs
    }
    
    override fun validateArguments(genericArgs: Map<GenericArgument, String>): List<String> {
        val errors = super.validateArguments(genericArgs).toMutableList()
        
        // Claude Code-specific validation
        val hasModel = genericArgs.containsKey(GenericArgument.MODEL)
        val hasPrompt = genericArgs.containsKey(GenericArgument.PROMPT)
        
        if (!hasModel) {
            errors.add("Model argument is required for Claude Code")
        }
        
        if (!hasPrompt) {
            errors.add("Prompt argument is required for Claude Code")
        }
        
        // Validate generation parameters
        genericArgs[GenericArgument.MAX_TOKENS]?.let { maxTokens ->
            if (maxTokens.toIntOrNull() == null || maxTokens.toInt() <= 0) {
                errors.add("Max tokens must be a positive integer")
            }
        }
        
        genericArgs[GenericArgument.TEMPERATURE]?.let { temperature ->
            if (temperature.toDoubleOrNull() == null || temperature.toDouble() < 0.0 || temperature.toDouble() > 2.0) {
                errors.add("Temperature must be between 0.0 and 2.0")
            }
        }
        
        genericArgs[GenericArgument.TOP_P]?.let { topP ->
            if (topP.toDoubleOrNull() == null || topP.toDouble() < 0.0 || topP.toDouble() > 1.0) {
                errors.add("Top-p must be between 0.0 and 1.0")
            }
        }
        
        // Check for unsupported arguments
        val unsupportedArgs = listOf(
            GenericArgument.YES_FLAG,
            GenericArgument.EDIT_FORMAT,
            GenericArgument.LINT_COMMAND,
            GenericArgument.AUTO_COMMIT,
            GenericArgument.DIRTY_COMMIT,
            GenericArgument.REASONING_EFFORT,
            GenericArgument.DEACTIVATE_REPO_MAP,
            GenericArgument.INCLUDE_CHANGE_CONTEXT,
            GenericArgument.SHELL_MODE,
            GenericArgument.STRUCTURED_MODE
        )
        
        unsupportedArgs.forEach { arg ->
            if (genericArgs.containsKey(arg)) {
                errors.add("Argument '$arg' is not supported by Claude Code")
            }
        }
        
        return errors
    }
    
    /**
     * Gets the default arguments that should always be included for Claude Code.
     * @return List of default arguments
     */
    fun getDefaultArguments(): List<String> {
        return emptyList() // Claude Code doesn't have required default arguments
    }
    
    /**
     * Gets the mode-specific arguments for Claude Code.
     * @param mode The mode identifier
     * @return List of mode-specific arguments
     */
    fun getModeArguments(mode: String): List<String> {
        return when (mode.lowercase()) {
            "edit" -> listOf("--edit")
            "code-review" -> listOf("--code-review")
            "debug" -> listOf("--debug")
            "architect" -> listOf("--architect")
            else -> emptyList()
        }
    }
    
    /**
     * Gets the feature-specific arguments for Claude Code.
     * @param feature The feature to enable
     * @return List of feature-specific arguments
     */
    fun getFeatureArguments(feature: String): List<String> {
        return when (feature.lowercase()) {
            "streaming" -> listOf("--stream")
            "verbose" -> listOf("--verbose")
            "quiet" -> listOf("--quiet")
            "debug" -> listOf("--debug")
            "thinking" -> listOf("--thinking")
            "file-ops" -> listOf("--file-ops")
            "shell" -> listOf("--shell")
            "web-search" -> listOf("--web-search")
            else -> emptyList()
        }
    }
    
    /**
     * Gets the generation parameters as argument list.
     * @param maxTokens Maximum tokens
     * @param temperature Temperature parameter
     * @param topP Top-p parameter
     * @return List of generation parameter arguments
     */
    fun getGenerationParameterArguments(
        maxTokens: Int,
        temperature: Double,
        topP: Double
    ): List<String> {
        return listOf(
            "--max-tokens", maxTokens.toString(),
            "--temperature", temperature.toString(),
            "--top-p", topP.toString()
        )
    }
}