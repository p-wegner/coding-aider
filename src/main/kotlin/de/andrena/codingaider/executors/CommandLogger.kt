package de.andrena.codingaider.executors

import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings

class CommandLogger(private val settings: AiderSettings, private val commandData: CommandData) {
    fun getCommandString(includeNewlines: Boolean = true, dockerManager: DockerContainerManager? = null): String =
        if (settings.verboseCommandLogging) {
            val useDockerAider = commandData.useDockerAider ?: settings.useDockerAider
            val executionStrategy = if (useDockerAider) {
                DockerAiderExecutionStrategy(dockerManager ?: DockerContainerManager())
            } else {
                NativeAiderExecutionStrategy()
            }
            val commandArgs = executionStrategy.buildCommand(commandData)
            val commandString = "Command: ${commandArgs.joinToString(" ")}"
            if (includeNewlines) "$commandString\n\n" else commandString
        } else ""

    fun prependCommandToOutput(output: String): String = "${getCommandString()}$output"
}
