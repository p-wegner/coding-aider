package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.executors.strategies.AiderExecutionStrategy
import de.andrena.codingaider.executors.strategies.DockerAiderExecutionStrategy
import de.andrena.codingaider.executors.strategies.NativeAiderExecutionStrategy
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(
    private val project: Project,
    private val commandData: CommandData,
    private val apiKeyChecker: ApiKeyChecker = service<DefaultApiKeyChecker>()
) {
    private val settings = getInstance()
    private val dockerManager = DockerContainerManager()
    private val useDockerAider: Boolean
        get() = commandData.options.useDockerAider ?: settings.useDockerAider
    private val executionStrategy: AiderExecutionStrategy by lazy {
        if (useDockerAider) DockerAiderExecutionStrategy(
            project,
            dockerManager,
            apiKeyChecker,
            settings
        ) else NativeAiderExecutionStrategy(project, apiKeyChecker, settings)
    }

    fun execute() {
        val terminalView = TerminalToolWindowManager.getInstance(project)
        val terminalSession = terminalView.createLocalShellWidget(project.basePath, "Aider", true)

        prepareEnvironment(terminalSession)

        val command = executionStrategy.buildCommand(commandData).joinToString(" ")
        terminalSession.executeCommand(command)
    }

    private fun prepareEnvironment(terminalSession: ShellTerminalWidget) {
        if (useDockerAider) {
            // Remove DOCKER_HOST if it's set, allowing Docker to use its default configuration
            terminalSession.executeCommand("set DOCKER_HOST=")
        }

        // Set API key environment variables
        apiKeyChecker.getAllApiKeyNames().forEach { keyName ->
            apiKeyChecker.getApiKeyValue(keyName)?.let { value ->
                terminalSession.executeCommand("set $keyName=$value")
            }
        }
    }
}
