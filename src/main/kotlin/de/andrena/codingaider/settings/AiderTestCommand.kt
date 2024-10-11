package de.andrena.codingaider.settings

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.CommandObserver
import de.andrena.codingaider.executors.api.LiveUpdateExecutor

class AiderTestCommand() {
    fun execute(observer: CommandObserver?, useDockerAider: Boolean) {
        val commandData = CommandData(
            message = "",
            useYesFlag = false,
            llm = "",
            additionalArgs = "--help",
            files = emptyList(),
            isShellMode = false,
            lintCmd = "",
            deactivateRepoMap = false,
            editFormat = "",
            projectPath = "",
            useDockerAider = useDockerAider  // Add this line
        )
        LiveUpdateExecutor(commandData).apply {
            observer?.let { addObserver(it) }
            execute()
        }
    }
}
