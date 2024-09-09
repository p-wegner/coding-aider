package de.andrena.codingaider.command

object AiderCommandBuilder {
    fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean): List<String> {
        return buildList {
            add("aider")
            if (commandData.llm.isNotEmpty()) {
                add(commandData.llm)
            }
            commandData.files.forEach { fileData ->
                if (fileData.isReadOnly) {
                    add("--read")
                } else {
                    add("--file")
                }
                add(fileData.filePath)
            }
            if (commandData.useYesFlag) add("--yes")
            if (commandData.editFormat.isNotEmpty()) {
                add("--edit-format")
                add(commandData.editFormat)
            }
            if (!isShellMode) {
                add("-m")
                add("\"${commandData.message}\"")
                add("--no-suggest-shell-commands")
                // try different encoding when issue is reproducible
//                add("--encoding \"UTF-16\"")
                add("--no-pretty")
            }
            if (commandData.additionalArgs.isNotEmpty()) {
                add(commandData.additionalArgs)
            }
            if (commandData.lintCmd.isNotEmpty()) {
                add("--lint-cmd")
                add("\"${commandData.lintCmd}\"")
            }
            if (commandData.deactivateRepoMap) {
                add("--map-tokens 0")
            }
        }
    }
}
