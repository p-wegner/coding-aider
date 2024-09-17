package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import kotlinx.coroutines.runBlocking

class SimpleExecutor(private val project: Project, private val commandData: CommandData) {
    fun execute(): String = runBlocking { CommandExecutor(project, commandData).executeCommand() }
}
