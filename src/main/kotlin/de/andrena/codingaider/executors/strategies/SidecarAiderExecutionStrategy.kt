package de.andrena.codingaider.executors.strategies

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings

class SidecarAiderExecutionStrategy(
    project: Project,
    private val settings: AiderSettings
) : AiderExecutionStrategy(project) {

    override fun buildCommand(commandData: CommandData): MutableList<String> {
        return (listOf(settings.aiderExecutablePath) + buildCommonArgs(commandData, settings)).toMutableList()
    }

    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        if (settings.enableLocalModelCostMap) {
            processBuilder.environment()["LITELLM_LOCAL_MODEL_COST_MAP"] = "True"
        }
    }

    override fun cleanupAfterExecution() {
        // Cleanup handled by AiderProcessManager
    }
}
