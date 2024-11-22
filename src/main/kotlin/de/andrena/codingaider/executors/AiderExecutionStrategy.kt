package de.andrena.codingaider.executors

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderDefaults
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.CustomLlmProviderService
import de.andrena.codingaider.settings.LlmProviderType
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.ApiKeyManager
import de.andrena.codingaider.utils.GitUtils.findGitRoot
import java.io.File

abstract class AiderExecutionStrategy(protected val project: Project) {
    abstract fun buildCommand(commandData: CommandData): List<String>
    abstract fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData)
    abstract fun cleanupAfterExecution()
    fun buildCommonArgs(commandData: CommandData, settings: AiderSettings): List<String> {
        return buildList {
            // Handle model selection based on provider type
            if (commandData.llm.isNotEmpty()) {
                val customProvider = CustomLlmProviderService.getInstance().getProvider(commandData.llm)
                if (customProvider != null) {
                    add("--model")
                    add(customProvider.prefixedModelName)
                } else {
                    if (commandData.llm.startsWith("--")) {
                        add(commandData.llm)
                    } else {
                        add("--model")
                        add(commandData.llm)

                    }
                }
                commandData.files.forEach { fileData ->
                    val fileArgument = if (fileData.isReadOnly) "--read" else "--file"
                    add(fileArgument)
                    add(fileData.filePath)
                }
                if (commandData.useYesFlag) add("--yes")
                if (commandData.editFormat.isNotEmpty()) {
                    add("--edit-format")
                    add(commandData.editFormat)
                }
                if (!commandData.isShellMode) {
                    add("--no-suggest-shell-commands")
                    add("--no-pretty")
                    add("--no-fancy-input")
                }
                if (commandData.additionalArgs.isNotEmpty()) {
                    addAll(commandData.additionalArgs.split(" "))
                }
                if (commandData.lintCmd.isNotEmpty()) {
                    add("--lint-cmd")
                    add(commandData.lintCmd)
                }
                if (commandData.deactivateRepoMap) {
                    add("--map-tokens")
                    add("0")
                }
                if (commandData.options.autoCommit != null) {
                    if (commandData.options.autoCommit) {
                        add("--auto-commits")
                    } else {
                        add("--no-auto-commits")
                    }
                } else {
                    when (settings.autoCommits) {
                        AiderSettings.AutoCommitSetting.ON -> add("--auto-commits")
                        AiderSettings.AutoCommitSetting.OFF -> add("--no-auto-commits")
                        AiderSettings.AutoCommitSetting.DEFAULT -> {} // Do nothing, use Aider's default
                    }
                }
                if (commandData.options.dirtyCommits != null) {
                    if (commandData.options.dirtyCommits) {
                        add("--dirty-commits")
                    } else {
                        add("--no-dirty-commits")
                    }
                } else {
                    when (settings.dirtyCommits) {
                        AiderSettings.DirtyCommitSetting.ON -> add("--dirty-commits")
                        AiderSettings.DirtyCommitSetting.OFF -> add("--no-dirty-commits")
                        AiderSettings.DirtyCommitSetting.DEFAULT -> {} // Do nothing, use Aider's default
                    }
                }
                if (settings.includeChangeContext) {
                    add("--commit-prompt")
                    add(getCommitPrompt())
                }
                if (commandData.sidecarMode) {
                    return@buildList
                }
                when (commandData.aiderMode) {
                    AiderMode.NORMAL -> {
                        add("-m")
                        add(commandData.message)
                    }

                    AiderMode.STRUCTURED -> {
                        add("-m")
                        add(project.service<AiderPlanService>().createAiderPlanSystemPrompt(commandData))
                    }

                    AiderMode.ARCHITECT -> {
                        add("-m")
                        add("/architect ${commandData.message}")
                    }

                    else -> {}
                }
            }
        }

    }

}
class NativeAiderExecutionStrategy(
    project: Project,
    private val apiKeyChecker: ApiKeyChecker,
    private val settings: AiderSettings
) : AiderExecutionStrategy(project) {

    override fun buildCommand(commandData: CommandData): List<String> {
        return listOf(settings.aiderExecutablePath) + buildCommonArgs(commandData, settings)
    }

    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        setApiKeyEnvironmentVariables(processBuilder, apiKeyChecker, commandData)
    }

    override fun cleanupAfterExecution() {
        // No specific cleanup needed for native execution
    }
}

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
        val customProvider = project.service<CustomLlmProviderService>().getProvider(commandData.llm)
        when (customProvider?.type) {
            LlmProviderType.OLLAMA -> {
                // For Ollama, we need to ensure network access to the host
                // TODO: set env like in setApiKeyEnvironmentVariables
                dockerArgs.addAll(listOf("--network", "host"))
            }

            LlmProviderType.OPENAI -> {
                // TODO: set env like in setApiKeyEnvironmentVariables
            }

            LlmProviderType.OPENROUTER -> {
                // TODO: set env like in setApiKeyEnvironmentVariables
            }

            null -> {} // No special configuration needed
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
        val gitRoot = findGitRoot(File(projectPath))
        val locations = listOfNotNull(
            gitRoot?.let { File(it, ".aider.conf.yml") },
            File(projectPath, ".aider.conf.yml"),
            File(System.getProperty("user.home"), ".aider.conf.yml")
        )

        return locations.firstOrNull { it.exists() }
    }
}


fun setApiKeyEnvironmentVariables(
    processBuilder: ProcessBuilder,
    apiKeyChecker: ApiKeyChecker,
    commandData: CommandData
) {
    val environment = processBuilder.environment()

    val customProvider = CustomLlmProviderService.getInstance().getProvider(commandData.llm)
    when {
        customProvider != null -> {
            // Set provider-specific environment variables
            when (customProvider.type) {
                LlmProviderType.OPENAI -> {
                    ApiKeyManager.getCustomModelKey(customProvider.name)?.let { apiKey ->
                        environment["OPENAI_API_KEY"] = apiKey
                        if (customProvider.baseUrl.isNotEmpty()) {
                            environment["OPENAI_API_BASE"] = customProvider.baseUrl
                        }
                    }
                }

                LlmProviderType.OLLAMA -> {
                    environment["OLLAMA_HOST"] = customProvider.baseUrl
                }

                LlmProviderType.OPENROUTER -> {
                    ApiKeyManager.getCustomModelKey(customProvider.name)?.let { apiKey ->
                        environment["OPENROUTER_API_KEY"] = apiKey
                    }
                }
            }
        }

        else -> {
            // Set standard API keys
            apiKeyChecker.getAllApiKeyNames().forEach { keyName ->
                apiKeyChecker.getApiKeyValue(keyName)?.let { value ->
                    environment[keyName] = value
                }
            }
        }
    }

}

private fun getCommitPrompt(): String {
    // the prompt in the Aider CLI https://github.com/paul-gauthier/aider/blob/main/aider/prompts.py extended with prompt context
    val basePrompt = """
        You are an expert software engineer.
        Review the provided context and diffs which are about to be committed to a git repo.
        Review the diffs carefully.
        Generate a commit message for those changes.
        The commit message MUST use the imperative tense.
        The commit message should be structured as follows: <type>: <description>
        Use these for <type>: fix, feat, build, chore, ci, docs, style, refactor, perf, test
        Reply with JUST the commit message, without quotes, comments, questions, etc!
    """.trimIndent()

    val extendedPrompt = """
        Additional to this commit message, the next lines after the message should include the prompt of the USER that led to the change 
        and the files (without content) that were given as context. Make sure the compact commit message is clearly separated from the additional information. Example:
        feat: Add a button to the login page
        
        USER: Create a button to allow users to login
        FILES: login.html, login.css
    """.trimIndent()

    return "$basePrompt\n$extendedPrompt"
}
