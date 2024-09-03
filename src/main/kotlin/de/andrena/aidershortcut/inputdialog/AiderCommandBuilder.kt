package de.andrena.aidershortcut.inputdialog

import de.andrena.aidershortcut.CommandData

object AiderCommandBuilder {
    fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean): List<String> {
        return buildList {
            add("aider")
            add(commandData.selectedCommand)
            commandData.filePaths.forEach { filePath ->
                add("--file")
                add(filePath)
            }
            if (commandData.useYesFlag) add("--yes")
            if (!isShellMode) {
                add("-m")
                add(commandData.message)
                add("--no-suggest-shell-commands")
            }
            commandData.readOnlyFiles.forEach { readOnlyFile ->
                add("--read")
                add(readOnlyFile)
            }
            if (commandData.additionalArgs.isNotEmpty()) {
                addAll(commandData.additionalArgs.split(" "))
            }
        }
    }
}