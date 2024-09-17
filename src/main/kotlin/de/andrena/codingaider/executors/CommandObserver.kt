package de.andrena.codingaider.executors

interface CommandObserver {
    fun onCommandStart(command: String) {}
    fun onCommandProgress(output: String, runningTime: Long) {}
    fun onCommandComplete(output: String, exitCode: Int) {}
    fun onCommandError(error: String) {}
    fun onUserInputRequired(prompt: String): String? = null
    fun onUserConfirmationRequired(prompt: String): Boolean = false
}

interface CommandSubject {
    fun addObserver(observer: CommandObserver): Boolean
    fun removeObserver(observer: CommandObserver): Boolean
    fun notifyObservers(event: (CommandObserver) -> Unit)
}
