package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData

class SimpleExecutor(private val project: Project, private val commandData: CommandData) {
    fun execute(): String = CommandExecutor(project, commandData).executeCommand()
}
