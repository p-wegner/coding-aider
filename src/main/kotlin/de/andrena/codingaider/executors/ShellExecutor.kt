package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(
    private val project: Project,
    private val commandData: CommandData
) {
    private val settings = AiderSettings.getInstance(project)
    private val dockerManager = DockerContainerManager()
    private val useDockerAider: Boolean
        get() = commandData.useDockerAider ?: settings.useDockerAider
    private val executionStrategy: AiderExecutionStrategy by lazy {
        if (useDockerAider) DockerAiderExecutionStrategy(dockerManager) else NativeAiderExecutionStrategy()
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
        ApiKeyChecker.getAllApiKeyNames().forEach { keyName ->
            ApiKeyChecker.getApiKeyValue(keyName)?.let { value ->
                terminalSession.executeCommand("set $keyName=$value")
            }
        }
    }
}
