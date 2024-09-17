package de.andrena.codingaider.executors

import kotlinx.coroutines.CompletableDeferred


interface CommandObserver {
    fun onCommandStart(command: String) {}
    fun onCommandProgress(output: String, runningTime: Long) {}
    fun onCommandComplete(output: String, exitCode: Int) {}
    fun onCommandError(error: String) {}
    suspend fun onUserInputRequired(prompt: String): CompletableDeferred<String?> {
        return CompletableDeferred(null)
    }

    suspend fun onUserConfirmationRequired(prompt: String): CompletableDeferred<Boolean> {
        return CompletableDeferred(false)
    }
}

interface CommandSubject {
    fun addObserver(observer: CommandObserver): Boolean
    fun removeObserver(observer: CommandObserver): Boolean
    suspend fun notifyObservers(event: suspend (CommandObserver) -> Unit)
}

class TimeoutException(message: String) : Exception(message)

sealed class UserResponse {
    data class Input(val value: String) : UserResponse()
    data class Confirmation(val value: Boolean) : UserResponse()
    data object NoResponse : UserResponse()
}
