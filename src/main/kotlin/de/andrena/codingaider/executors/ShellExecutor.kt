package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(
    private val project: Project,
    private val commandData: CommandData
) {
    fun execute() {
        val terminalView = TerminalToolWindowManager.getInstance(project)
        val terminalSession = terminalView.createLocalShellWidget(project.basePath, "Aider", true)

        val command =
            AiderCommandBuilder.buildAiderCommand(commandData, true, AiderSettings.getInstance(project).useDockerAider)
                .joinToString(" ")

        if (AiderSettings.getInstance(project).useDockerAider) {
            // Remove DOCKER_HOST if it's set, allowing Docker to use its default configuration
            terminalSession.executeCommand("set DOCKER_HOST=")
        }

        terminalSession.executeCommand(command)
    }
}
