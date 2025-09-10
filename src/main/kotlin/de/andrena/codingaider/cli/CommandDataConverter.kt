package de.andrena.codingaider.cli

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.inputdialog.AiderMode

/**
 * Utility class for converting between old CommandData and new generic command structures.
 * This provides a bridge during the refactoring process.
 */
object CommandDataConverter {
    
    /**
     * Converts old CommandData to GenericCommandData
     * @param commandData The old CommandData to convert
     * @return The converted GenericCommandData
     */
    fun toGenericCommandData(commandData: CommandData): GenericCommandData {
        return GenericCommandData(
            prompt = commandData.message,
            model = commandData.llm,
            files = commandData.files,
            options = toGenericCommandOptions(commandData.options),
            cliMode = toCliMode(commandData.aiderMode),
            projectPath = commandData.projectPath,
            // TODO 11.09.2025 pwegner: add workingdir if needed
            workingDir = commandData.projectPath,
            planId = commandData.planId,
            startTime = commandData.startTime
        )
    }
    
    /**
     * Converts GenericCommandData to old CommandData
     * @param genericCommandData The GenericCommandData to convert
     * @return The converted old CommandData
     */
    fun fromGenericCommandData(genericCommandData: GenericCommandData): CommandData {
        return CommandData(
            message = genericCommandData.prompt,
            useYesFlag = genericCommandData.options.useYesFlag,
            llm = genericCommandData.model,
            additionalArgs = genericCommandData.additionalArgs.values.joinToString(" "),
            files = genericCommandData.files,
            lintCmd = genericCommandData.options.lintCommand ?: "",
            deactivateRepoMap = genericCommandData.options.deactivateRepoMap,
            editFormat = genericCommandData.options.editFormat ?: "",
            projectPath = genericCommandData.projectPath,
            options = fromGenericCommandOptions(genericCommandData.options),
            aiderMode = fromCliMode(genericCommandData.cliMode),
            sidecarMode = genericCommandData.options.sidecarMode,
            planId = genericCommandData.planId,
            startTime = genericCommandData.startTime
        )
    }
    
    /**
     * Converts old CommandOptions to GenericCommandOptions
     * @param commandOptions The old CommandOptions to convert
     * @return The converted GenericCommandOptions
     */
    fun toGenericCommandOptions(commandOptions: CommandOptions): GenericCommandOptions {
        return GenericCommandOptions(
            useDocker = commandOptions.useDockerAider ?: false,
            disablePresentation = commandOptions.disablePresentation,
            commitHashToCompareWith = commandOptions.commitHashToCompareWith,
            autoCommit = commandOptions.autoCommit,
            dirtyCommit = commandOptions.dirtyCommits,
            promptAugmentation = commandOptions.promptAugmentation
        )
    }
    
    /**
     * Converts GenericCommandOptions to old CommandOptions
     * @param genericOptions The GenericCommandOptions to convert
     * @return The converted old CommandOptions
     */
    fun fromGenericCommandOptions(genericOptions: GenericCommandOptions): CommandOptions {
        return CommandOptions(
            disablePresentation = genericOptions.disablePresentation,
            useDockerAider = if (genericOptions.useDocker) true else null,
            commitHashToCompareWith = genericOptions.commitHashToCompareWith,
            autoCommit = genericOptions.autoCommit,
            dirtyCommits = genericOptions.dirtyCommit,
            promptAugmentation = genericOptions.promptAugmentation
        )
    }
    
    /**
     * Converts old AiderMode to CliMode
     * @param aiderMode The old AiderMode to convert
     * @return The converted CliMode
     */
    fun toCliMode(aiderMode: AiderMode): CliMode {
        return when (aiderMode) {
            AiderMode.NORMAL -> CliMode.NORMAL
            AiderMode.STRUCTURED -> CliMode.STRUCTURED
            AiderMode.ARCHITECT -> CliMode.ARCHITECT
            AiderMode.SHELL -> CliMode.SHELL
        }
    }
    
    /**
     * Converts CliMode to old AiderMode
     * @param cliMode The CliMode to convert
     * @return The converted old AiderMode
     */
    fun fromCliMode(cliMode: CliMode): AiderMode {
        return when (cliMode) {
            CliMode.NORMAL -> AiderMode.NORMAL
            CliMode.STRUCTURED -> AiderMode.STRUCTURED
            CliMode.ARCHITECT -> AiderMode.ARCHITECT
            CliMode.SHELL -> AiderMode.SHELL
            else -> AiderMode.NORMAL // Default fallback for unsupported modes
        }
    }
    
    /**
     * Creates a GenericCommandData from CLI-specific command
     * @param cliCommand The CLI-specific command
     * @param genericCommandData The original generic command data
     * @return A new GenericCommandData with CLI-specific information
     */
    fun withCliCommand(
        cliCommand: CliSpecificCommand,
        genericCommandData: GenericCommandData
    ): GenericCommandData {
        val additionalArgs = mutableMapOf<String, String>()
        
        // Add CLI-specific metadata as additional arguments
        cliCommand.metadata.forEach { (key, value) ->
            additionalArgs["cli_$key"] = value
        }
        
        // Add execution-specific information
        if (cliCommand.useDocker) {
            additionalArgs["use_docker"] = "true"
            additionalArgs["docker_image"] = cliCommand.dockerImage
        }
        
        if (cliCommand.useSidecar) {
            additionalArgs["use_sidecar"] = "true"
            additionalArgs["sidecar_process_id"] = cliCommand.sidecarProcessId ?: ""
        }
        
        return genericCommandData.copy(
            additionalArgs = genericCommandData.additionalArgs + additionalArgs,
            workingDir = cliCommand.workingDirectory
        )
    }
    
    /**
     * Validates compatibility between old and new structures
     * @param commandData The old CommandData to validate
     * @return List of compatibility warnings
     */
    fun validateCompatibility(commandData: CommandData): List<String> {
        val warnings = mutableListOf<String>()
        
        // Check for unsupported modes
        if (commandData.aiderMode == AiderMode.SHELL) {
            warnings.add("Shell mode may not be supported by all CLI tools")
        }
        
        // Check for Aider-specific features
        if (commandData.editFormat.isNotBlank()) {
            warnings.add("Edit format is Aider-specific and may not be supported by all CLI tools")
        }
        
        if (commandData.deactivateRepoMap) {
            warnings.add("Repository map deactivation is Aider-specific and may not be supported by all CLI tools")
        }
        
        if (commandData.sidecarMode) {
            warnings.add("Sidecar mode is Aider-specific and may not be supported by all CLI tools")
        }
        
        return warnings
    }
}