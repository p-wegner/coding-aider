package de.andrena.codingaider.executors

interface CommandObserver {
    fun onCommandStart(message: String) {}
    fun onCommandProgress(message: String, runningTime: Long) {}
    fun onCommandComplete(message: String, exitCode: Int) {}
    fun onCommandError(message: String) {}
}

interface CommandSubject {
    fun addObserver(observer: CommandObserver): Boolean
    fun removeObserver(observer: CommandObserver): Boolean
    fun notifyObservers(event: (CommandObserver) -> Unit)
}
