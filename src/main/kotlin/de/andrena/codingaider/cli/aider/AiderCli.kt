package de.andrena.codingaider.cli.aider

import de.andrena.codingaider.cli.*
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSpecificSettings

/**
 * Aider CLI implementation.
 * Provides the interface for interacting with the Aider AI coding assistant.
 */
class AiderCli : BaseCliInterface() {
    
    private val modelHandler = AiderModelHandler()
    private val argumentMapper = AiderArgumentMapper()
    
    override fun getExecutableName(): String = "aider"
    
    override val supportedFeatures: Set<CliFeature> = setOf(
        CliFeature.FILE_OPERATIONS,
        CliFeature.READ_ONLY_FILES,
        CliFeature.YES_FLAG,
        CliFeature.EDIT_FORMATS,
        CliFeature.LINT_COMMAND,
        CliFeature.AUTO_COMMIT,
        CliFeature.DIRTY_COMMIT,
        CliFeature.SHELL_MODE,
        CliFeature.STRUCTURED_MODE,
        CliFeature.REASONING_EFFORT,
        CliFeature.PLUGIN_BASED_EDITS,
        CliFeature.REPO_MAP_DEACTIVATION,
        CliFeature.CHANGE_CONTEXT,
        CliFeature.SIDECAR_MODE,
        CliFeature.DOCKER_SUPPORT,
        CliFeature.CUSTOM_API_ENDPOINTS,
        CliFeature.MODEL_REASONING,
        CliFeature.WEB_CRAWL,
        CliFeature.DOCUMENTATION_GENERATION,
        CliFeature.PROJECT_ANALYSIS,
        CliFeature.CODE_REVIEW,
        CliFeature.REFACTORING_SUGGESTIONS,
        CliFeature.TEST_GENERATION,
        CliFeature.DEBUGGING_ASSISTANCE,
        CliFeature.PERFORMANCE_OPTIMIZATION
    )
    
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
    
    override fun buildCommand(commandData: CommandData): List<String> {
        val command = mutableListOf<String>()
        
        // Add executable
        command.add(getExecutableName())
        
        // Add model if specified
        if (commandData.llm.isNotBlank()) {
            command.add("--model")
            command.add(commandData.llm)
        }
        
        // Add prompt
        command.add("-m")
        command.add(commandData.message)
        
        // Add files
        commandData.files.forEach { fileData ->
            val arg = if (fileData.isReadOnly) "--read" else "--file"
            command.add(arg)
            command.add(fileData.filePath)
        }
        
        // Add yes flag if enabled
        if (commandData.useYesFlag) {
            command.add("--yes")
        }
        
        // Add edit format if specified
        if (commandData.editFormat.isNotBlank()) {
            command.add("--edit-format")
            command.add(commandData.editFormat)
        }
        
        // Add lint command if specified
        if (commandData.lintCmd.isNotBlank()) {
            command.add("--lint-cmd")
            command.add(commandData.lintCmd)
        }
        
        // Add deactivate repo map if enabled
        if (commandData.deactivateRepoMap) {
            command.add("--no-repo-map")
        }
        
        // Include change context is not available in current CommandOptions structure
        // TODO: Add this to CommandOptions when needed
        
        // Add auto-commit if enabled
        if (commandData.options.autoCommit == true) {
            command.add("--auto-commits")
        }
        
        // Add dirty commits if enabled
        if (commandData.options.dirtyCommits == true) {
            command.add("--dirty-commits")
        }
        
        // Add mode-specific arguments
        when (commandData.aiderMode) {
            de.andrena.codingaider.inputdialog.AiderMode.ARCHITECT -> {
                command.add("--architect")
            }
            de.andrena.codingaider.inputdialog.AiderMode.SHELL -> {
                command.add("--shell")
            }
            de.andrena.codingaider.inputdialog.AiderMode.STRUCTURED -> {
                command.add("--architect")
            }
            de.andrena.codingaider.inputdialog.AiderMode.NORMAL -> {
                // No additional arguments for normal mode
            }
        }
        
        // Add additional arguments
        if (commandData.additionalArgs.isNotBlank()) {
            command.addAll(commandData.additionalArgs.split(" "))
        }
        
        // Add common Aider flags
        command.add("--no-suggest-shell-commands")
        command.add("--no-pretty")
        
        return command
    }
    
    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        super.addCommonEnvironmentVariables(processBuilder, commandData)
        
        val env = processBuilder.environment()
        val aiderSettings = AiderSpecificSettings.getInstance()
        
        // Add Aider-specific environment variables
        env["AIDER_MODEL"] = commandData.llm.ifBlank { aiderSettings.defaultAiderMode.name }
        
        // Add reasoning effort if specified
        if (aiderSettings.reasoningEffort.isNotBlank()) {
            env["AIDER_REASONING_EFFORT"] = aiderSettings.reasoningEffort
        }
        
        // Set working directory if specified
        // Note: working directory handling is CLI-specific and not in CommandData
        // TODO: Add working directory support when needed
    }
    
    override fun cleanupAfterExecution() {
        // Aider-specific cleanup if needed
        logDebug("Aider cleanup completed")
    }
    
    override fun validateCommandData(commandData: CommandData): List<String> {
        val errors = super.validateCommandData(commandData).toMutableList()
        
        // Aider-specific validation
        if (commandData.message.isBlank()) {
            errors.add("Message cannot be blank for Aider")
        }
        
        if (commandData.aiderMode == de.andrena.codingaider.inputdialog.AiderMode.SHELL && 
            !supportsFeature(CliFeature.SHELL_MODE)) {
            errors.add("Shell mode is not supported in this configuration")
        }
        
        return errors
    }
    
    override fun getModelHandler(): CliModelHandler = modelHandler
    
    override fun getArgumentMapper(): CliArgumentMapper = argumentMapper
}