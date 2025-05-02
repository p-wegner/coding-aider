package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.ChainedAiderCommand
import de.andrena.codingaider.executors.api.IDEBasedExecutor

@Service(Service.Level.PROJECT)
class ChainCommandService(val project: Project) {

    /**
     * Execute a chain of Aider commands, where each command can use the output of the previous as input.
     * The chain is executed synchronously, and each step's output is passed to the next step if requested.
     * The last output is returned.
     */
    fun executeChainedCommands(
        commands: List<ChainedAiderCommand>
    ): String? {
        var lastOutput: String? = null
        for (chained in commands) {
            val cmd = if (chained.transformOutputToInput != null && lastOutput != null) {
                chained.commandData.copy(
                    message = chained.transformOutputToInput.invoke(lastOutput)
                )
            } else {
                chained.commandData
            }
            val executor = IDEBasedExecutor(project, cmd)
            executor.execute()
            executor.isFinished().await()
            lastOutput = executor.getFinalOutput()
        }
        return lastOutput
    }

}

