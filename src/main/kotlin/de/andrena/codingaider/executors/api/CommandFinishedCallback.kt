package de.andrena.codingaider.executors.api

@FunctionalInterface
fun interface CommandFinishedCallback {
    fun onCommandFinished(success: Boolean)
}
