package de.andrena.codingaider.settings

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.settings.AiderSettings

class AiderTestCommand(private val project: Project, private val command: String) {
    fun execute(): MarkdownDialog? {
        val settings = AiderSettings.getInstance(project)
        val commandData = CommandData(
            message = "Aider test command",
            useYesFlag = false,
            llm = "",
            additionalArgs = command,
            files = emptyList(),
            isShellMode = false,
            lintCmd = "",
            deactivateRepoMap = settings.deactivateRepoMap
        )

        val executor = IDEBasedExecutor(project, commandData)
        val dialog = executor.execute()
        return dialog
    }
}
