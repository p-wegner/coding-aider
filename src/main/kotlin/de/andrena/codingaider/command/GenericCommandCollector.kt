package de.andrena.codingaider.command

import com.intellij.openapi.project.Project
import de.andrena.codingaider.cli.CliFactory
import de.andrena.codingaider.cli.CliMode
import de.andrena.codingaider.cli.GenericCommandData
import de.andrena.codingaider.cli.GenericCommandOptions
import de.andrena.codingaider.settings.GenericCliSettings
import de.andrena.codingaider.utils.FileTraversal

/**
 * Generic command collector that works with different CLI tools.
 * This abstracts the command data collection process to support multiple CLI tools.
 */
object GenericCommandCollector {
    
    /**
     * Collects command data from a generic dialog.
     * @param dialog The generic dialog to collect data from
     * @param project The project context
     * @return Generic command data
     */
    fun collectFromDialog(dialog: GenericDialog, project: Project): GenericCommandData {
        val genericSettings = GenericCliSettings.getInstance()
        val cliInterface = CliFactory.getCurrentCli()
        
        // Build generic command options
        val options = GenericCommandOptions(
            useYesFlag = dialog.getYesFlag(),
            additionalOptions = dialog.getAdditionalArguments()
        )
        
        // Apply CLI-specific defaults
        val cliOptions = applyCliDefaults(options, dialog.getCliToolName())
        
        return GenericCommandData(
            prompt = dialog.getPrompt(),
            model = dialog.getModel(),
            files = dialog.getFiles(),
            options = cliOptions,
            cliMode = dialog.getMode(),
            projectPath = project.basePath ?: "",
            workingDir = getWorkingDirectory(project),
            additionalArgs = dialog.getAdditionalArguments()
        )
    }
    
    /**
     * Collects command data from individual parameters.
     * @param files List of files to include
     * @param prompt The prompt/message
     * @param project The project context
     * @param mode The execution mode
     * @param model The model to use (optional)
     * @param useYesFlag Whether to use yes flag (optional)
     * @param additionalArgs Additional arguments (optional)
     * @return Generic command data
     */
    fun collectFromParameters(
        files: List<FileData>,
        prompt: String,
        project: Project,
        mode: CliMode,
        model: String? = null,
        useYesFlag: Boolean = false,
        additionalArgs: Map<String, String> = emptyMap()
    ): GenericCommandData {
        val genericSettings = GenericCliSettings.getInstance()
        val cliInterface = CliFactory.getCurrentCli()
        
        // Use provided model or default from settings
        val resolvedModel = model ?: genericSettings.defaultModel
        
        // Build options
        val options = GenericCommandOptions(
            useYesFlag = useYesFlag,
            additionalOptions = additionalArgs
        )
        
        // Apply CLI-specific defaults
        val cliOptions = applyCliDefaults(options, genericSettings.selectedCli)
        
        return GenericCommandData(
            prompt = prompt,
            model = resolvedModel,
            files = files,
            options = cliOptions,
            cliMode = mode,
            projectPath = project.basePath ?: "",
            workingDir = getWorkingDirectory(project),
            additionalArgs = additionalArgs
        )
    }
    
    /**
     * Collects command data for shell mode execution.
     * @param files List of files to include
     * @param project The project context
     * @param model The model to use (optional)
     * @return Generic command data
     */
    fun collectForShellMode(
        files: List<FileData>,
        project: Project,
        model: String? = null
    ): GenericCommandData {
        val genericSettings = GenericCliSettings.getInstance()
        val cliInterface = CliFactory.getCurrentCli()
        
        // Check if shell mode is supported
        cliInterface?.supportsFeature(de.andrena.codingaider.cli.CliFeature.SHELL_MODE)?.let {
            if (!it) {
                throw UnsupportedOperationException("Shell mode is not supported by the selected CLI tool")
            }
        }
        
        // Use provided model or default from settings
        val resolvedModel = model ?: genericSettings.defaultModel
        
        // Build options for shell mode
        val options = GenericCommandOptions(
            useYesFlag = true, // Usually yes in shell mode
            additionalOptions = mapOf("shell_mode" to "true")
        )
        
        // Apply CLI-specific defaults
        val cliOptions = applyCliDefaults(options, genericSettings.selectedCli)
        
        return GenericCommandData(
            prompt = "Shell mode execution", // Default prompt for shell mode
            model = resolvedModel,
            files = files,
            options = cliOptions,
            cliMode = CliMode.SHELL,
            projectPath = project.basePath ?: "",
            workingDir = getWorkingDirectory(project),
            additionalArgs = mapOf("shell" to "true")
        )
    }
    
    /**
     * Collects command data for structured mode execution.
     * @param files List of files to include
     * @param prompt The prompt/message
     * @param project The project context
     * @param planId Optional plan ID
     * @param model The model to use (optional)
     * @return Generic command data
     */
    fun collectForStructuredMode(
        files: List<FileData>,
        prompt: String,
        project: Project,
        planId: String? = null,
        model: String? = null
    ): GenericCommandData {
        val genericSettings = GenericCliSettings.getInstance()
        val cliInterface = CliFactory.getCurrentCli()
        
        // Check if structured mode is supported
        cliInterface?.supportsFeature(de.andrena.codingaider.cli.CliFeature.STRUCTURED_MODE)?.let {
            if (!it) {
                throw UnsupportedOperationException("Structured mode is not supported by the selected CLI tool")
            }
        }
        
        // Use provided model or default from settings
        val resolvedModel = model ?: genericSettings.defaultModel
        
        // Build options for structured mode
        val options = GenericCommandOptions(
            useYesFlag = false, // Usually want confirmation in structured mode
            additionalOptions = mapOf("structured_mode" to "true")
        )
        
        // Apply CLI-specific defaults
        val cliOptions = applyCliDefaults(options, genericSettings.selectedCli)
        
        return GenericCommandData(
            prompt = prompt,
            model = resolvedModel,
            files = files,
            options = cliOptions,
            cliMode = CliMode.STRUCTURED,
            projectPath = project.basePath ?: "",
            workingDir = getWorkingDirectory(project),
            planId = planId,
            additionalArgs = mapOf("structured" to "true")
        )
    }
    
    /**
     * Applies CLI-specific defaults to the command options.
     * @param options The base options
     * @param cliName The CLI tool name
     * @return Options with CLI-specific defaults applied
     */
    private fun applyCliDefaults(options: GenericCommandOptions, cliName: String): GenericCommandOptions {
        return when (cliName.lowercase()) {
            "aider" -> applyAiderDefaults(options)
            "claude", "claude-code" -> applyClaudeDefaults(options)
            else -> options
        }
    }
    
    /**
     * Applies Aider-specific defaults.
     * @param options The base options
     * @return Options with Aider defaults applied
     */
    private fun applyAiderDefaults(options: GenericCommandOptions): GenericCommandOptions {
        val aiderSettings = de.andrena.codingaider.settings.AiderSpecificSettings.getInstance()
        
        return options.copy(
            editFormat = aiderSettings.editFormat.ifBlank { options.editFormat },
            lintCommand = aiderSettings.lintCmd.ifBlank { options.lintCommand },
            deactivateRepoMap = aiderSettings.deactivateRepoMap,
            includeChangeContext = aiderSettings.includeChangeContext,
            sidecarMode = aiderSettings.state.useSidecarMode
        )
    }
    
    /**
     * Applies Claude Code-specific defaults.
     * @param options The base options
     * @return Options with Claude defaults applied
     */
    private fun applyClaudeDefaults(options: GenericCommandOptions): GenericCommandOptions {
        val claudeSettings = de.andrena.codingaider.settings.ClaudeCodeSpecificSettings.getInstance()
        
        return options.copy(
            additionalOptions = options.additionalOptions + mapOf(
                "max_tokens" to claudeSettings.maxTokens.toString(),
                "temperature" to claudeSettings.temperature.toString(),
                "top_p" to claudeSettings.topP.toString()
            )
        )
    }
    
    /**
     * Gets the working directory for the project.
     * @param project The project
     * @return The working directory path
     */
    private fun getWorkingDirectory(project: Project): String {
        val projectSettings = project.getService(de.andrena.codingaider.settings.AiderProjectSettings::class.java)
        return projectSettings?.workingDirectory ?: project.basePath ?: ""
    }
    
    /**
     * Validates the command data for the current CLI tool.
     * @param commandData The command data to validate
     * @return List of validation errors, empty if valid
     */
    fun validateCommandData(commandData: GenericCommandData): List<String> {
        val cliInterface = CliFactory.getCurrentCli()
        val errors = mutableListOf<String>()
        
        // Basic validation
        errors.addAll(commandData.validate())
        
        // CLI-specific validation
        cliInterface?.let { cli ->
            errors.addAll(cli.validateCommandData(
                de.andrena.codingaider.cli.CommandDataConverter.fromGenericCommandData(commandData)
            ))
        }
        
        return errors
    }
    
    /**
     * Converts generic command data to the old CommandData format for backward compatibility.
     * @param genericCommandData The generic command data
     * @return Legacy command data
     */
    fun toLegacyCommandData(genericCommandData: GenericCommandData): CommandData {
        return de.andrena.codingaider.cli.CommandDataConverter.fromGenericCommandData(genericCommandData)
    }
    
    /**
     * Converts legacy command data to generic format.
     * @param legacyCommandData The legacy command data
     * @return Generic command data
     */
    fun fromLegacyCommandData(legacyCommandData: CommandData): GenericCommandData {
        return de.andrena.codingaider.cli.CommandDataConverter.toGenericCommandData(legacyCommandData)
    }
}