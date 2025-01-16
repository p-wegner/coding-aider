package de.andrena.codingaider.executors.strategies

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderDefaults
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.CustomLlmProviderService
import de.andrena.codingaider.settings.LlmProviderType
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.ApiKeyManager
import de.andrena.codingaider.utils.GitUtils
import java.io.File

class DockerAiderExecutionStrategy(
    project: Project,
    private val dockerManager: DockerContainerManager,
    private val apiKeyChecker: ApiKeyChecker,
    private val settings: AiderSettings
) : AiderExecutionStrategy(project) {

    override fun buildCommand(commandData: CommandData): List<String> {
        val dockerArgs = mutableListOf(
            "docker", "run", "-i",
            // For sidecar mode, we want to keep the container running
            if (settings.useSidecarMode) "--restart=always" else "--rm",
            "-w", "/app",
            "--cidfile", dockerManager.getCidFilePath()
        ).apply {
            if (commandData.projectPath.isNotEmpty()) {
                add("-v")
                add("${commandData.projectPath}:/app")
            }
            if (commandData.isShellMode || settings.useSidecarMode) {
                add("-t")
            }

            // For sidecar mode, add a long-running command to keep container alive
            if (settings.useSidecarMode) {
                add("-d")  // Detached mode
                add("--name")
                add("aider-sidecar")  // Named container for easier management
            }
        }
        if (settings.mountAiderConfInDocker) {
            findAiderConfFile(commandData.projectPath)?.let { confFile ->
                dockerArgs.addAll(listOf("-v", "${confFile.absolutePath}:/app/.aider.conf.yml"))
            }
        }

        // Add API key environment variables to Docker run command
        apiKeyChecker.getApiKeysForDocker().forEach { (keyName, value) ->
            dockerArgs.addAll(listOf("-e", "$keyName=$value"))
        }

        // Add provider-specific Docker configurations
        val customProvider = service<CustomLlmProviderService>().getProvider(commandData.llm)
        when (customProvider?.type) {
            LlmProviderType.OLLAMA -> {
                // For Ollama, we need to ensure network access to the host
                dockerArgs.addAll(listOf("--network", "host"))
                customProvider.baseUrl.takeIf { it.isNotEmpty() }?.let { baseUrl ->
                    dockerArgs.addAll(listOf("-e", "OLLAMA_HOST=$baseUrl"))
                }
            }

            LlmProviderType.OPENAI -> {
                ApiKeyManager.getCustomModelKey(customProvider.name)?.let { apiKey ->
                    dockerArgs.addAll(listOf("-e", "OPENAI_API_KEY=$apiKey"))
                    if (customProvider.baseUrl.isNotEmpty()) {
                        dockerArgs.addAll(listOf("-e", "OPENAI_API_BASE=${customProvider.baseUrl}"))
                    }
                }
            }

            LlmProviderType.OPENROUTER -> {
                ApiKeyManager.getCustomModelKey(customProvider.name)?.let { apiKey ->
                    dockerArgs.addAll(listOf("-e", "OPENROUTER_API_KEY=$apiKey"))
                }
            }

            LlmProviderType.VERTEX_EXPERIMENTAL -> {
                // Pass Google Cloud credentials if available
                ApiKeyManager.getCustomModelKey(customProvider.name)?.let { credentials ->
                    dockerArgs.addAll(listOf("-e", "GOOGLE_APPLICATION_CREDENTIALS=/tmp/google_credentials.json"))
                    // Mount credentials file into container
                    dockerArgs.addAll(listOf("-v", "$credentials:/tmp/google_credentials.json:ro"))
                }
                if (customProvider.projectId.isNotEmpty()) {
                    dockerArgs.addAll(listOf("-e", "VERTEXAI_PROJECT=${customProvider.projectId}"))
                }
                if (customProvider.location.isNotEmpty()) {
                    dockerArgs.addAll(listOf("-e", "VERTEXAI_LOCATION=${customProvider.location}"))
                }
            }

            LlmProviderType.CUSTOM_AIDERMODEL, null -> {} // No special configuration needed
        }

        // Mount files outside the project
        commandData.files.forEach { fileData ->
            if (!fileData.filePath.startsWith(commandData.projectPath)) {
                val containerPath = "/extra/${File(fileData.filePath).name}"
                dockerArgs.addAll(listOf("-v", "${fileData.filePath}:$containerPath"))
            }
        }

        dockerArgs.add("${AiderDefaults.DOCKER_IMAGE}:${settings.dockerImageTag}")

        return dockerArgs + buildCommonArgs(commandData, settings).map { arg ->
            commandData.files.fold(arg) { acc, fileData ->
                if (!fileData.filePath.startsWith(commandData.projectPath)) {
                    acc.replace(fileData.filePath, "/extra/${File(fileData.filePath).name}")
                } else {
                    acc.replace(fileData.filePath, "/app${fileData.filePath.removePrefix(commandData.projectPath)}")
                }
            }
        }
    }

    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        // Remove DOCKER_HOST to use the default Docker host
        processBuilder.environment().remove("DOCKER_HOST")
    }

    override fun cleanupAfterExecution() {
        dockerManager.removeCidFile()
    }

    fun findAiderConfFile(projectPath: String): File? {
        val gitRoot = GitUtils.findGitRoot(File(projectPath))
        val locations = listOfNotNull(
            gitRoot?.let { File(it, ".aider.conf.yml") },
            File(projectPath, ".aider.conf.yml"),
            File(System.getProperty("user.home"), ".aider.conf.yml")
        )

        return locations.firstOrNull { it.exists() }
    }
}