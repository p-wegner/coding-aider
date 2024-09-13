package de.andrena.codingaider.command

import de.andrena.codingaider.settings.AiderDefaults.DOCKER_IMAGE
import de.andrena.codingaider.utils.ApiKeyChecker
import java.io.File

object AiderCommandBuilder {
    fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean, useDockerAider: Boolean): List<String> {
        return buildList {
            val projectPath = commandData.projectPath
            if (useDockerAider) {
                addDockerRunCommand(projectPath, commandData)
            } else {
                add("aider")
            }
            if (commandData.llm.isNotEmpty()) {
                add(commandData.llm)
            }
            commandData.files.forEach { fileData ->
                val fileArgument = if (fileData.isReadOnly) "--read" else "--file"
                add(fileArgument)
                val filePath = determineNeededFilePath(useDockerAider, fileData, projectPath)
                add("$filePath")
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

    private fun determineNeededFilePath(
        useDockerAider: Boolean,
        fileData: FileData,
        projectPath: String
    ) = if (useDockerAider) {
        if (fileData.filePath.startsWith(projectPath)) {
            "/app/${
                fileData.filePath.removePrefix(projectPath).replace('\\', '/')
                    .removePrefix("/")
            }"
        } else {
            "/extra/${File(fileData.filePath).name}"
        }
    } else {
        fileData.filePath
    }

    private fun MutableList<String>.addDockerRunCommand(
        projectPath: String,
        commandData: CommandData,
        dockerManager: DockerContainerManager
    ) {
        add("docker")
        add("run")
        add("-i")
        add("--rm")
        // Mount the entire project workspace
        add("-v")
        add("$projectPath:/app")
        mountFilesOutsideProject(commandData.files, projectPath)
        add("-w")
        add("/app")
        // Add environment variables for API keys
        ApiKeyChecker.getAllApiKeyNames().forEach { keyName ->
            ApiKeyChecker.getApiKeyValue(keyName)?.let { value ->
                add("-e")
                add("$keyName=$value")
            }
        }
        // Add cidfile option
        add("--cidfile")
        add(dockerManager.getCidFilePath())
        add(DOCKER_IMAGE)
    }

    private fun MutableList<String>.mountFilesOutsideProject(
        files: List<FileData>,
        projectPath: String
    ) {
        files.forEach { fileData ->
            if (!fileData.filePath.startsWith(projectPath)) {
                val containerPath = "/extra/${File(fileData.filePath).name}"
                add("-v")
                add("${fileData.filePath}:$containerPath")
            }
        }
    }
}
