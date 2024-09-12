package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(
    private val project: Project,
    private val commandData: CommandData
) {
    fun execute() {
        val terminalView = TerminalToolWindowManager.getInstance(project)
        val terminalSession = terminalView.createLocalShellWidget(project.basePath, "Aider", true)

        val command = AiderCommandBuilder.buildAiderCommand(commandData, true, AiderSettings.getInstance(project).useDockerAider).joinToString(" ")
        
        if (AiderSettings.getInstance(project).useDockerAider) {
            terminalSession.executeCommand("export DOCKER_HOST=unix:///var/run/docker.sock")
        }
        
        terminalSession.executeCommand(command)
    }
}
