package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(private val project: Project, private val commandData: CommandData, private val parentComponent: Component?) {
    fun execute() {
        val terminalView = TerminalToolWindowManager.getInstance(project)
        val terminalSession = terminalView.createLocalShellWidget(project.basePath, "Aider", true)

        val command = AiderCommandBuilder.buildAiderCommand(commandData, true).joinToString(" ")
        terminalSession.executeCommand(command)
    }
}
