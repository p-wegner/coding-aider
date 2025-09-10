package de.andrena.codingaider.cli.claude

import de.andrena.codingaider.cli.*
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.ClaudeCodeSpecificSettings
import java.io.File

/**
 * Claude Code CLI implementation.
 * Provides the interface for interacting with the Claude Code AI coding assistant.
 */
class ClaudeCodeCli : BaseCliInterface() {
    
    private val modelHandler = ClaudeCodeModelHandler()
    private val argumentMapper = ClaudeCodeArgumentMapper()
    
    override fun getExecutableName(): String = "claude"
    
    override val supportedFeatures: Set<CliFeature> = setOf(
        CliFeature.FILE_OPERATIONS,
        CliFeature.READ_ONLY_FILES,
        CliFeature.CODE_GENERATION,
        CliFeature.CODE_ANALYSIS,
        CliFeature.DOCUMENTATION_GENERATION,
        CliFeature.PROJECT_ANALYSIS,
        CliFeature.CODE_REVIEW,
        CliFeature.REFACTORING_SUGGESTIONS,
        CliFeature.TEST_GENERATION,
        CliFeature.DEBUGGING_ASSISTANCE,
        CliFeature.MULTI_FILE_PROCESSING,
        CliFeature.FILE_EDITING,
        CliFeature.FILE_CREATION,
        CliFeature.CONTEXT_AWARENESS,
        CliFeature.PERFORMANCE_OPTIMIZATION
        // Note: Claude Code doesn't support all Aider-specific features
    )
    
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
    
    override fun buildCommand(commandData: CommandData): List<String> {
        val command = mutableListOf<String>()
        val settings = ClaudeCodeSpecificSettings.getInstance()
        
        // Add executable
        command.add(getExecutableName())
        
        // Add model if specified
        if (commandData.llm.isNotBlank()) {
            command.add("--model")
            command.add(commandData.llm)
        } else {
            // Use default model from settings
        }
        
        // Add prompt
        command.add("-p")
        command.add(commandData.message)
        
        // Add files
        commandData.files.forEach { fileData ->
            val arg = if (fileData.isReadOnly) "--read" else "--file"
            command.add(arg)
            command.add(fileData.filePath)
        }
        
        // Add generation parameters
        command.add("--max-tokens")
        command.add(settings.maxTokens.toString())
        
        command.add("--temperature")
        command.add(settings.temperature.toString())
        
        command.add("--top-p")
        command.add(settings.topP.toString())
        
        // Add system prompt if specified
        if (settings.systemPrompt.isNotBlank()) {
            command.add("--system")
            command.add(settings.systemPrompt)
        }
        
        // Add user context if specified
        if (settings.userContext.isNotBlank()) {
            command.add("--context")
            command.add(settings.userContext)
        }
        
        // Add feature flags
        if (settings.enableStreaming) {
            command.add("--stream")
        }
        
        if (settings.verboseOutput) {
            command.add("--verbose")
        }
        
        if (settings.enableThinking) {
            command.add("--thinking")
        }
        
        if (settings.enableEditMode) {
            command.add("--edit")
        }
        
        if (settings.enableFileOperations) {
            command.add("--file-ops")
        }
        
        if (settings.enableShellCommands) {
            command.add("--shell")
        }
        
        if (settings.enableWebSearch) {
            command.add("--web-search")
        }
        
        // Add working directory if specified
//        if (commandData.workingDir.isNotBlank()) {
//            command.add("--workdir")
//            command.add(commandData.workingDir)
//        }
        
        // Add additional arguments
        if (settings.claudeAdditionalArgs.isNotBlank()) {
            command.addAll(settings.claudeAdditionalArgs.split(" "))
        }
        
        return command
    }
    
    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        super.addCommonEnvironmentVariables(processBuilder, commandData)
        
        val env = processBuilder.environment()
        val settings = ClaudeCodeSpecificSettings.getInstance()
        
        // Add Claude-specific environment variables

        // Set working directory if specified
//        if (commandData.workingDir.isNotBlank()) {
//            processBuilder.directory(File(commandData.workingDir))
//        }
        
        // Add safe mode flag
        if (settings.claudeFlags.safeMode) {
            env["CLAUDE_SAFE_MODE"] = "true"
        }
    }
    
    override fun cleanupAfterExecution() {
        // Claude-specific cleanup if needed
        logDebug("Claude Code cleanup completed")
    }
    
    override fun validateCommandData(commandData: CommandData): List<String> {
        val errors = super.validateCommandData(commandData).toMutableList()
        
        // Claude-specific validation
        if (commandData.message.isBlank()) {
            errors.add("Message cannot be blank for Claude Code")
        }
        
        // Check for unsupported Aider-specific features
        if (commandData.useYesFlag) {
            errors.add("Yes flag is not supported by Claude Code")
        }
        
        if (commandData.editFormat.isNotBlank()) {
            errors.add("Edit format is not supported by Claude Code")
        }
        
        if (commandData.lintCmd.isNotBlank()) {
            errors.add("Lint command is not supported by Claude Code")
        }
        
        if (commandData.deactivateRepoMap) {
            errors.add("Repository map deactivation is not supported by Claude Code")
        }
        
        if (commandData.options.autoCommit == true) {
            errors.add("Auto-commit is not supported by Claude Code")
        }
        
        if (commandData.options.dirtyCommits == true) {
            errors.add("Dirty commits are not supported by Claude Code")
        }
        
        // Validate Claude Code specific settings
        val settings = ClaudeCodeSpecificSettings.getInstance()
        if (settings.maxTokens <= 0) {
            errors.add("Max tokens must be positive")
        }
        
        if (settings.temperature < 0.0 || settings.temperature > 2.0) {
            errors.add("Temperature must be between 0.0 and 2.0")
        }
        
        if (settings.topP < 0.0 || settings.topP > 1.0) {
            errors.add("Top-p must be between 0.0 and 1.0")
        }
        
        return errors
    }
    
    override fun getModelHandler(): CliModelHandler = modelHandler
    
    override fun getArgumentMapper(): CliArgumentMapper = argumentMapper
}