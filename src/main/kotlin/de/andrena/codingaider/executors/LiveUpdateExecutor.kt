package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import kotlinx.coroutines.runBlocking

class LiveUpdateExecutor(private val project: Project, private val commandData: CommandData) :
    CommandSubject by GenericCommandSubject() {
    fun execute(): String {
        val executor = CommandExecutor(project, commandData)
        executor.addObserver(object : CommandObserver {
            override fun onCommandStart(message: String) {
                runBlocking {
                    notifyObservers { it.onCommandStart(message) }
                }
            }

            override fun onCommandProgress(output: String, runningTime: Long) {
                runBlocking {
                    notifyObservers { it.onCommandProgress(output, runningTime) }
                }
            }

            override fun onCommandComplete(output: String, exitCode: Int) {
                runBlocking { notifyObservers { it.onCommandComplete(output, exitCode) } }
            }

            override fun onCommandError(errorMessage: String) {
                runBlocking { notifyObservers { it.onCommandError(errorMessage) } }
            }
        })
        return runBlocking { executor.executeCommand() }
    }
}
