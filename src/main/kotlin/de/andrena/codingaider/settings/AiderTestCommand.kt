package de.andrena.codingaider.settings

import com.intellij.openapi.project.ProjectManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
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
            lintCmd = "",
            deactivateRepoMap = false,
            editFormat = "",
            projectPath = "",
            options = CommandOptions(useDockerAider = useDockerAider)
        )
        LiveUpdateExecutor(commandData, ProjectManager.getInstance().defaultProject).apply {
            observer?.let { addObserver(it) }
            execute()
        }
    }
}
