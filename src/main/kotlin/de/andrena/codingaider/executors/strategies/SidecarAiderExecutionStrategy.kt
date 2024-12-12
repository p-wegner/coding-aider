package de.andrena.codingaider.executors.strategies

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings

class SidecarAiderExecutionStrategy(
    project: Project,
    private val settings: AiderSettings
) : AiderExecutionStrategy(project) {

    override fun buildCommand(commandData: CommandData): List<String> {
        return listOf(settings.aiderExecutablePath) + buildCommonArgs(commandData, settings)
    }

    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        // No special environment prep needed for sidecar mode
    }

    override fun cleanupAfterExecution() {
        // Cleanup handled by AiderProcessManager
    }
}