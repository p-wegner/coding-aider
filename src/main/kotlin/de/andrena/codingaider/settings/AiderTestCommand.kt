package de.andrena.codingaider.settings

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.LiveUpdateExecutor
import de.andrena.codingaider.executors.CommandObserver

class AiderTestCommand(private val project: Project) {
    fun execute(observer: CommandObserver): String {
        val settings = AiderSettings.getInstance(project)
        val commandData = CommandData(
            message = "",
            useYesFlag = false,
            llm = "",
            additionalArgs = "--help",
            files = emptyList(),
            isShellMode = false,
            lintCmd = "",
            deactivateRepoMap = settings.deactivateRepoMap,
            editFormat = "",
            projectPath = project.basePath ?: ""
        )

        val executor = LiveUpdateExecutor(project, commandData)
        executor.addObserver(observer)
        return executor.execute()
    }
}
