package de.andrena.codingaider.settings

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.SimpleExecutor

class AiderTestCommand(private val project: Project) {
    fun execute(): String {
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

        return SimpleExecutor(project, commandData).execute()
    }
}
