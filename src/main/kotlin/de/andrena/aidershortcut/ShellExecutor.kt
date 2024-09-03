package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ShellExecutor(private val project: Project, private val commandData: CommandData) {
    fun execute() {
        val terminalView = TerminalToolWindowManager.getInstance(project)
        val terminalSession = terminalView.createLocalShellWidget(project.basePath, "Aider", true)

        val command = buildAiderCommand(commandData, true)
        terminalSession.executeCommand(command)
    }

    private fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean): String {
        return StringBuilder("aider ${commandData.selectedCommand}").apply {
            if (commandData.filePaths.isNotBlank()) append(" --file ${commandData.filePaths}")
            if (commandData.useYesFlag) append(" --yes")
            if (!isShellMode) {
                append(" -m \"${commandData.message}\"")
                append(" --no-suggest-shell-commands")
            }
            if (commandData.readOnlyFiles.isNotEmpty()) append(" --read ${commandData.readOnlyFiles.joinToString(" ")}")
            if (commandData.additionalArgs.isNotEmpty()) append(" ${commandData.additionalArgs}")
        }.toString()
    }
}


