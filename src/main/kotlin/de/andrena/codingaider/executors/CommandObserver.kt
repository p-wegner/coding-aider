package de.andrena.codingaider.executors

interface CommandObserver {
    fun onCommandStart(command: String)
    fun onCommandProgress(output: String, runningTime: Long)
    fun onCommandComplete(output: String, exitCode: Int)
    fun onCommandError(error: String)
}

interface CommandSubject {
    fun addObserver(observer: CommandObserver): Boolean
    fun removeObserver(observer: CommandObserver): Boolean
    fun notifyObservers(event: (CommandObserver) -> Unit)
}
