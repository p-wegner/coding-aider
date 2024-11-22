package de.andrena.codingaider.executors

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker

class CommandLogger(
    private val project: Project,
    private val settings: AiderSettings,
    private val commandData: CommandData,
    private val apiKeyChecker: ApiKeyChecker = service<DefaultApiKeyChecker>()
) {
    fun getCommandString(includeNewlines: Boolean = true, dockerManager: DockerContainerManager? = null): String =
        if (settings.verboseCommandLogging) {
            val useDockerAider = commandData.options.useDockerAider ?: settings.useDockerAider
            val executionStrategy = if (useDockerAider) {
                DockerAiderExecutionStrategy(project,dockerManager ?: DockerContainerManager(), apiKeyChecker, settings)
            } else {
                NativeAiderExecutionStrategy(project,apiKeyChecker, settings)
            }
            val commandArgs = executionStrategy.buildCommand(commandData)
            val commandString = "Command: ${commandArgs.joinToString(" ")}"
            if (includeNewlines) "$commandString\n\n" else commandString
        } else ""

    fun prependCommandToOutput(output: String): String = "${getCommandString()}$output"
}
