package de.andrena.codingaider.executors

import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings

class CommandLogger(private val settings: AiderSettings, private val commandData: CommandData) {
    fun getCommandString(includeNewlines: Boolean = true): String =
        if (settings.verboseCommandLogging) {
            val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false, settings.useDockerAider)
            val commandString = if (settings.useDockerAider) {
                val dockerArgs = commandArgs.takeWhile { it != "paulgauthier/aider" }
                val aiderArgs = commandArgs.dropWhile { it != "paulgauthier/aider" }
                "Docker Command: ${dockerArgs.joinToString(" ")}\n" +
                "Aider Command: ${aiderArgs.joinToString(" ")}"
            } else {
                "Command: ${commandArgs.joinToString(" ")}"
            }
            if (includeNewlines) "$commandString\n\n" else commandString
        } else ""

    fun prependCommandToOutput(output: String): String = "${getCommandString()}$output"
}
