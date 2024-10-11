package de.andrena.codingaider.executors.api

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.CommandExecutor

class SimpleExecutor(private val project: Project, private val commandData: CommandData) {
    fun execute(): String = CommandExecutor(commandData,project).executeCommand()
}
