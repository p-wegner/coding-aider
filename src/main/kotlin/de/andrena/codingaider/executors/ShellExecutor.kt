package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.ApiKeyManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(
    private val project: Project,
    private val commandData: CommandData
) {
    private val settings = AiderSettings.getInstance(project)
    fun execute() {
        val terminalView = TerminalToolWindowManager.getInstance(project)
        val terminalSession = terminalView.createLocalShellWidget(project.basePath, "Aider", true)

        val useDockerAider = settings.useDockerAider

        if (useDockerAider) {
            // Remove DOCKER_HOST if it's set, allowing Docker to use its default configuration
            terminalSession.executeCommand("set DOCKER_HOST=")
        } else {
            setApiKeyEnvsForIDEBasedKeys(terminalSession)
        }

        val command = AiderCommandBuilder.buildAiderCommand(commandData, true, useDockerAider)
            .joinToString(" ")

        terminalSession.executeCommand(command)
    }

    private fun setApiKeyEnvsForIDEBasedKeys(terminalSession: ShellTerminalWidget) {
        ApiKeyChecker.getAllApiKeyNames().forEach { keyName ->
            val apiKey = ApiKeyManager.getApiKey(keyName)
            if (apiKey != null) {
                terminalSession.executeCommand("set $keyName=$apiKey")
            }
        }
    }
}
