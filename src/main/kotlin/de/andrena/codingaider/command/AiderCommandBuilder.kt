package de.andrena.codingaider.command

object AiderCommandBuilder {
    fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean): List<String> {
        return buildList {
            add("aider")
            if (commandData.llm.isNotEmpty()) {
                add(commandData.llm)
            }
            commandData.files.forEach { fileData ->
                val fileArgument = if (fileData.isReadOnly) "--read" else "--file"
                add("$fileArgument \"${fileData.filePath}\"")
            }
            if (commandData.useYesFlag) add("--yes")
            if (commandData.editFormat.isNotEmpty()) {
                add("--edit-format \"${commandData.editFormat}\"")
            }
            if (!isShellMode) {
                add("--no-suggest-shell-commands")
                add("--no-pretty")
            }
            if (commandData.additionalArgs.isNotEmpty()) {
                add(commandData.additionalArgs)
            }
            if (commandData.lintCmd.isNotEmpty()) {
                add("--lint-cmd \"${commandData.lintCmd}\"")
            }
            if (commandData.deactivateRepoMap) {
                add("--map-tokens 0")
            }
            if (!isShellMode) {
                add("-m \"${commandData.message}\"")
            }
        }
    }
}
