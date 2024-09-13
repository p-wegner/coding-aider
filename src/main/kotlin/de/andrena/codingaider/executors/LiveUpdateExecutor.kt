package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData

class LiveUpdateExecutor(private val project: Project, private val commandData: CommandData) : CommandSubject by GenericCommandSubject() {
    fun execute(): String {
        val executor = CommandExecutor(project, commandData)
        executor.addObserver(object : CommandObserver {
            override fun onCommandStart(message: String) {
                notifyObservers { it.onCommandStart(message) }
            }

            override fun onCommandProgress(output: String, runningTime: Long) {
                notifyObservers { it.onCommandProgress(output, runningTime) }
            }

            override fun onCommandComplete(output: String, exitCode: Int) {
                notifyObservers { it.onCommandComplete(output, exitCode) }
            }

            override fun onCommandError(errorMessage: String) {
                notifyObservers { it.onCommandError(errorMessage) }
            }
        })
        return executor.executeCommand()
    }
}
package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData

class LiveUpdateExecutor(private val project: Project, private val commandData: CommandData) : CommandSubject by GenericCommandSubject() {
    fun execute(): String {
        val executor = CommandExecutor(project, commandData)
        var output = StringBuilder()
        
        executor.addObserver(object : CommandObserver {
            override fun onCommandStart(message: String) {
                notifyObservers { it.onCommandStart(message) }
            }

            override fun onCommandProgress(progressOutput: String, runningTime: Long) {
                output.append(progressOutput)
                notifyObservers { it.onCommandProgress(progressOutput, runningTime) }
            }

            override fun onCommandComplete(finalOutput: String, exitCode: Int) {
                output.append(finalOutput)
                notifyObservers { it.onCommandComplete(finalOutput, exitCode) }
            }

            override fun onCommandError(errorMessage: String) {
                output.append(errorMessage)
                notifyObservers { it.onCommandError(errorMessage) }
            }
        })
        
        executor.executeCommand()
        return output.toString()
    }
}
