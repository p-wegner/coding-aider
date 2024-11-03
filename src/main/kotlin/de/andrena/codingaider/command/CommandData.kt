package de.andrena.codingaider.command


/**
 * Represents the data required to execute an Aider command.
 *
 * @property message The main message or instruction for Aider.
 * @property useYesFlag If true, automatically confirms all prompts.
 * @property llm The language model to be used (e.g., "gpt-4"). Models prefixed with -- can directly
 * be passed as options to aider, others have to be passed with --model {llm}
 * @property additionalArgs Any additional command-line arguments for Aider.
 * @property files List of files to be included in the Aider command.
 * @property isShellMode If true, enables shell mode for Aider.
 * @property lintCmd Command to run for linting the code.
 * @property deactivateRepoMap If true, disables the repository mapping feature.
 * @property editFormat Specifies the format for edit instructions (e.g., "diff").
 * @property options Contains optional parameters for the command, including whether to disable presentation of changes.
 */
data class CommandData(
    val message: String,
    val useYesFlag: Boolean,
    val llm: String,
    val additionalArgs: String,
    val files: List<FileData>,
    val isShellMode: Boolean,
    val lintCmd: String,
    val deactivateRepoMap: Boolean = false,
    val editFormat: String = "",
    val projectPath: String,
    val options: CommandOptions = CommandOptions.DEFAULT,
    val structuredMode: Boolean = false
)

/**
 * Represents the optional parameters for the Aider command.
 *
 * @property useDockerAider If true, uses the Docker Aider image ignoring the settings.
 * @property disablePresentation If true, disables the presentation of changes after command execution.
 */
data class CommandOptions(
    val disablePresentation: Boolean = false,
    val useDockerAider: Boolean = false
) {
    companion object {
        val DEFAULT = CommandOptions()
    }
}
