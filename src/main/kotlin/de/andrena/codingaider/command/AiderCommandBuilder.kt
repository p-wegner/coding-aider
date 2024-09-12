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
                add("-v")
                add("${System.getProperty("user.dir")}:/app")
                add("-v")
                add("${System.getProperty("user.dir")}/.aider:/app/.aider")
                add("-v")
                add("${System.getProperty("user.dir")}/.aider-docs:/app/.aider-docs")
                add("-w")
                add("/app")
                // Add environment variables for API keys
                ApiKeyChecker.getAllLlmOptions().forEach { llm ->
                    val apiKeyName = ApiKeyChecker.getApiKeyForLlm(llm)
                    if (apiKeyName != null && System.getenv(apiKeyName) != null) {
                        add("-e")
                        add("$apiKeyName=${System.getenv(apiKeyName)}")
                    }
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
