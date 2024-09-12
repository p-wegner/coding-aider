package de.andrena.codingaider.command

import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.EnvFileReader
import java.io.File

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
                val envVars = mutableMapOf<String, String>()
                ApiKeyChecker.getAllLlmOptions().forEach { llm ->
                    val apiKeyName = ApiKeyChecker.getApiKeyForLlm(llm)
                    if (apiKeyName != null) {
                        val apiKeyValue = getApiKeyValue(apiKeyName)
                        if (apiKeyValue != null) {
                            envVars[apiKeyName] = apiKeyValue
                        }
                    }
                }
                envVars.forEach { (key, value) ->
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

    private fun getApiKeyValue(apiKeyName: String): String? {
        // Check environment variable
        System.getenv(apiKeyName)?.let { return it }

        // Check .env file in the user's home directory
        val homeEnvFile = File(System.getProperty("user.home"), ".env")
        EnvFileReader.readEnvFile(homeEnvFile)[apiKeyName]?.let { return it }

        // Check .env file in the current working directory
        val currentDirEnvFile = File(System.getProperty("user.dir"), ".env")
        EnvFileReader.readEnvFile(currentDirEnvFile)[apiKeyName]?.let { return it }

        return null
    }
}
