package de.andrena.codingaider.command

import de.andrena.codingaider.utils.ApiKeyChecker

object AiderCommandBuilder {
    fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean, useDockerAider: Boolean): List<String> {
        return buildList {
            if (useDockerAider) {
                add("docker")
                add("run")
                add("-i")
                add("--rm")
                // Mount the entire project workspace
                add("-v")
                add("${commandData.projectPath}:/app")
                add("-w")
                add("/app")
                // Add environment variables for API keys
                ApiKeyChecker.getApiKeysForDocker().forEach { (key, value) ->
                    add("-e")
                    add("$key=$value")
                }
                add("paulgauthier/aider")
            } else {
                add("aider")
            }
            if (commandData.llm.isNotEmpty()) {
                add(commandData.llm)
            }
            commandData.files.forEach { fileData ->
                val fileArgument = if (fileData.isReadOnly) "--read" else "--file"
                add(fileArgument)
                val filePath = if (useDockerAider) {
                    "/app/${
                        fileData.filePath.removePrefix(commandData.projectPath).replace('\\', '/')
                            .removePrefix("/")
                    }"
                } else {
                    fileData.filePath
                }
                add("\"$filePath\"")
            }
            if (commandData.useYesFlag) add("--yes")
            if (commandData.editFormat.isNotEmpty()) {
                add("--edit-format")
                add(commandData.editFormat)
            }
            if (!isShellMode) {
                add("--no-suggest-shell-commands")
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
                add("--map-tokens")
                add("0")
            }
            if (!isShellMode) {
                add("-m")
                add("\"${commandData.message}\"")
            }
        }
    }
}
