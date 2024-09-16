package de.andrena.codingaider.executors

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderDefaults
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.GitUtils.findGitRoot
import java.io.File

interface AiderExecutionStrategy {
    fun buildCommand(commandData: CommandData): List<String>
    fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData)
    fun cleanupAfterExecution()
}

class NativeAiderExecutionStrategy(
    private val apiKeyChecker: ApiKeyChecker,
    private val settings: AiderSettings
) : AiderExecutionStrategy {
    override fun buildCommand(commandData: CommandData): List<String> {
        return listOf("aider") + buildCommonArgs(commandData, settings)
    }

    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        setApiKeyEnvironmentVariables(processBuilder, apiKeyChecker)
    }

    override fun cleanupAfterExecution() {
        // No specific cleanup needed for native execution
    }
}

class DockerAiderExecutionStrategy(
    private val dockerManager: DockerContainerManager,
    private val apiKeyChecker: ApiKeyChecker,
    private val settings: AiderSettings
) : AiderExecutionStrategy {
    override fun buildCommand(commandData: CommandData): List<String> {
        val dockerArgs = mutableListOf(
            "docker", "run", "-i", "--rm", "-t",
            "-v", "${commandData.projectPath}:/app",
            "-w", "/app",
            "--cidfile", dockerManager.getCidFilePath()
        )

        if (settings.mountAiderConfInDocker) {
            findAiderConfFile(commandData.projectPath)?.let { confFile ->
                dockerArgs.addAll(listOf("-v", "${confFile.absolutePath}:/app/.aider.conf.yml"))
            }
        }

        // Add API key environment variables to Docker run command
        apiKeyChecker.getApiKeysForDocker().forEach { (keyName, value) ->
            dockerArgs.addAll(listOf("-e", "$keyName=$value"))
        }

        // Mount files outside the project
        commandData.files.forEach { fileData ->
            if (!fileData.filePath.startsWith(commandData.projectPath)) {
                val containerPath = "/extra/${File(fileData.filePath).name}"
                dockerArgs.addAll(listOf("-v", "${fileData.filePath}:$containerPath"))
            }
        }

        dockerArgs.add(AiderDefaults.DOCKER_IMAGE)

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

private fun buildCommonArgs(commandData: CommandData, settings: AiderSettings): List<String> {
    return buildList {
        if (commandData.llm.isNotEmpty()) add(commandData.llm)
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
        if (!commandData.isShellMode) {
            add("-m")
            add(commandData.message)
        }
        when (settings.autoCommits) {
            AiderSettings.AutoCommitSetting.ON -> add("--auto-commits")
            AiderSettings.AutoCommitSetting.OFF -> add("--no-auto-commits")
            AiderSettings.AutoCommitSetting.DEFAULT -> {} // Do nothing, use Aider's default
        }
        when (settings.dirtyCommits) {
            AiderSettings.DirtyCommitSetting.ON -> add("--dirty-commits")
            AiderSettings.DirtyCommitSetting.OFF -> add("--no-dirty-commits")
            AiderSettings.DirtyCommitSetting.DEFAULT -> {} // Do nothing, use Aider's default
        }
        if (settings.includeChangeContext) {
            add("--commit-prompt")
            add(getCommitPrompt())
        }
    }
}

private fun setApiKeyEnvironmentVariables(processBuilder: ProcessBuilder, apiKeyChecker: ApiKeyChecker) {
    val environment = processBuilder.environment()
    apiKeyChecker.getAllApiKeyNames().forEach { keyName ->
        apiKeyChecker.getApiKeyValue(keyName)?.let { value ->
            environment[keyName] = value
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
