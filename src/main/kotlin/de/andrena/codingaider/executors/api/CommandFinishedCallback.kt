package de.andrena.codingaider.executors.api

fun interface CommandFinishedCallback {
    fun onCommandFinished(success: Boolean)
}
