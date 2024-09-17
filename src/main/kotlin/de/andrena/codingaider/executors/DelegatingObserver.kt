package de.andrena.codingaider.executors

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

class DelegatingObserver(private val subject: CommandSubject) : CommandObserver {
    override fun onCommandStart(command: String) {
        runBlocking { subject.notifyObservers { it.onCommandStart(command) } }
    }

    override fun onCommandProgress(output: String, runningTime: Long) {
        runBlocking { subject.notifyObservers { it.onCommandProgress(output, runningTime) } }
    }

    override fun onCommandComplete(output: String, exitCode: Int) {
        runBlocking { subject.notifyObservers { it.onCommandComplete(output, exitCode) } }
    }

    override fun onCommandError(error: String) {
        runBlocking { subject.notifyObservers { it.onCommandError(error) } }
    }

    override suspend fun onUserInputRequired(prompt: String): CompletableDeferred<String?> {
        val responses = mutableListOf<CompletableDeferred<String?>>()
        subject.notifyObservers { observer ->
            responses.add(observer.onUserInputRequired(prompt))
        }
        return CompletableDeferred<String?>().apply {
            responses.firstOrNull { it.await() != null }?.await()?.let { complete(it) } ?: complete(null)
        }
    }

    override suspend fun onUserConfirmationRequired(prompt: String): CompletableDeferred<Boolean> {
        val responses = mutableListOf<CompletableDeferred<Boolean>>()
        subject.notifyObservers { observer ->
            responses.add(observer.onUserConfirmationRequired(prompt))
        }
        return CompletableDeferred<Boolean>().apply {
            responses.firstOrNull { it.await() }?.await()?.let { complete(it) } ?: complete(false)
        }
    }
}

