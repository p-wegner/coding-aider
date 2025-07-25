package de.andrena.codingaider.command

import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.utils.CommandDataCollector
import de.andrena.codingaider.utils.FileTraversal


/**
 * Represents the data required to execute an Aider command.
 *
 * @property message The main message or instruction for Aider.
 * @property useYesFlag If true, automatically confirms all prompts.
 * @property llm The language model to be used (e.g., "gpt-4"). Models prefixed with -- can directly
 * be passed as options to aider, others have to be passed with --model {llm}
 * @property additionalArgs Any additional command-line arguments for Aider.
 * @property files List of files to be included in the Aider command.
 * @property lintCmd Command to run for linting the code.
 * @property deactivateRepoMap If true, disables the repository mapping feature.
 * @property editFormat Specifies the format for edit instructions (e.g., "diff").
 * @property options Contains optional parameters for the command, including whether to disable presentation of changes.
 * @property aiderMode The mode to use for the command.
 * @property sidecarMode If true, uses the sidecar mode for the command.
 * @property planId The plan ID to use for the command.
 */
data class CommandData(
    val message: String,
    val useYesFlag: Boolean,
    val llm: String,
    val additionalArgs: String,
    val files: List<FileData>,
    val lintCmd: String,
    val deactivateRepoMap: Boolean = false,
    val editFormat: String = "",
    val projectPath: String,
    val options: CommandOptions = CommandOptions.DEFAULT,
    val aiderMode: AiderMode = AiderMode.NORMAL,
    val sidecarMode: Boolean = false,
    val planId: String? = null,
    val startTime: Long = System.currentTimeMillis()
) {

    val isShellMode: Boolean get() = aiderMode == AiderMode.SHELL
    val structuredMode: Boolean get() = aiderMode == AiderMode.STRUCTURED
}

/**
 * Represents the optional parameters for the Aider command.
 * Parameters that are null will use the value from the Plugins settings.
 *
 * @property useDockerAider If true, uses the Docker Aider image ignoring the settings.
 * @property disablePresentation If true, disables the presentation of changes after command execution.
 * @property commitHashToCompareWith If not null, compares the changes with the specified commit hash.
 * @property autoCloseDelay If not null, sets the auto close delay for the markdown dialog.
 * @property autoCommit If true, automatically commits the changes after the command execution.
 * @property dirtyCommits If true, will commit changes before the command execution.
 * @property promptAugmentation If true, returns a summarized output in xml tags after usual outputs.
 */
data class CommandOptions(
    val disablePresentation: Boolean = false,
    val useDockerAider: Boolean? = null,
    val commitHashToCompareWith: String? = null,
    val autoCloseDelay: Int? = null,
    val autoCommit: Boolean? = null,
    val dirtyCommits: Boolean? = null,
    val promptAugmentation: Boolean? = null,
) {

    companion object {
        val DEFAULT = CommandOptions()
    }
}

data class FileData(val filePath: String, val isReadOnly: Boolean) {
    val normalizedFilePath: String
        get() = FileTraversal.normalizedFilePath(filePath)

    fun hasSameNormalizedPath(other: FileData): Boolean = other.normalizedFilePath == normalizedFilePath
}

/**
 * Data class representing a single step in a multi-step Aider command chain.
 * Each step can optionally take the previous output as input.
 */
data class ChainedAiderCommand(
    val commandData: CommandData,
    val transformOutputToInput: ((String) -> String)? = null // If set, will use previous output as input
)


