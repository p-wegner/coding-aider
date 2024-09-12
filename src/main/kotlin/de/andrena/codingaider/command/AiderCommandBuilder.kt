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
                // Mount the entire workspace
                add("-v")
                add("${System.getProperty("user.dir")}:/app")
                // Mount the user's home directory
                add("-v")
                add("${System.getProperty("user.home")}:/root")
                // Mount specific Aider files
                add("-v")
                add("${System.getProperty("user.dir")}/.aider.chat.history.md:/app/.aider.chat.history.md")
                add("-v")
                add("${System.getProperty("user.dir")}/.aider.context.yaml:/app/.aider.context.yaml")
                add("-v")
                add("${System.getProperty("user.dir")}/.aider.input.history:/app/.aider.input.history")
                // Mount the .aider-docs folder
                add("-v")
                add("${System.getProperty("user.dir")}/.aider-docs:/app/.aider-docs")
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
                    "/app/${fileData.filePath.removePrefix(System.getProperty("user.dir")).replace('\\', '/').removePrefix("/")}"
                } else {
                    fileData.filePath
                }
                add(filePath)
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
