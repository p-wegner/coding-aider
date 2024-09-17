package de.andrena.codingaider.executors

import kotlinx.coroutines.CompletableDeferred

class ExecuteCommandFixedTimes(private val message: String, private var count: Long = 1) : CommandObserver {

    override suspend fun onUserInputRequired(prompt: String): CompletableDeferred<String?> {
        if (count > 0) {
            count -= 1
            return CompletableDeferred(message)
        } else return CompletableDeferred(null)
    }
}
