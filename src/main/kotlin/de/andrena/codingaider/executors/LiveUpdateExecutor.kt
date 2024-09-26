package de.andrena.codingaider.executors

import de.andrena.codingaider.command.CommandData

class LiveUpdateExecutor(private val commandData: CommandData) :
    CommandSubject by GenericCommandSubject() {
    fun execute(): String {
        val executor = CommandExecutor(commandData)
        executor.addObserver(object : CommandObserver {
            override fun onCommandStart(message: String) {
                notifyObservers { it.onCommandStart(message) }
            }

            override fun onCommandProgress(message: String, runningTime: Long) {
                notifyObservers { it.onCommandProgress(message, runningTime) }
            }

            override fun onCommandComplete(message: String, exitCode: Int) {
                notifyObservers { it.onCommandComplete(message, exitCode) }
            }

            override fun onCommandError(message: String) {
                notifyObservers { it.onCommandError(message) }
            }
        })
        return executor.executeCommand()
    }
}
