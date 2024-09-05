package de.andrena.codingaider.command

object AiderCommandBuilder {
    fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean): List<String> {
        return buildList {
            add("aider")
            add(commandData.llm)
            commandData.files.forEach { fileData ->
                if (fileData.isReadOnly) {
                    add("--read")
                } else {
                    add("--file")
                }
                add(fileData.filePath)
            }
            if (commandData.useYesFlag) add("--yes")
            if (!isShellMode) {
                add("-m")
                add(commandData.message)
                add("--no-suggest-shell-commands")
            }
            if (commandData.additionalArgs.isNotEmpty()) {
                addAll(commandData.additionalArgs.split(" "))
            }
            if (commandData.lintCmd.isNotEmpty()) {
                add("--lint-cmd")
                add("\"${commandData.lintCmd}\"")
            }
            if (commandData.lintCmd.isNotEmpty()) {
                add("--lint-cmd")
                add(commandData.lintCmd)
            }
        }
    }
}
