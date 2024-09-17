package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import kotlinx.coroutines.runBlocking

class LiveUpdateExecutor(private val project: Project, private val commandData: CommandData) :
    CommandSubject by GenericCommandSubject() {
    fun execute(): String {
        val executor = CommandExecutor(project, commandData)
        executor.addObserver(DelegatingObserver(this))
        return runBlocking { executor.executeCommand() }
    }
}
