package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.sidecar.AiderProcessManager
import reactor.core.publisher.Flux

class DefaultAiderProcessInteractor(private val project: Project) : AiderProcessInteractor {
    private val processManager = project.service<AiderProcessManager>()

    override fun sendCommandSync(command: String): String =
        processManager.sendCommandAsync(command)
            .collectList()
            .block()
            ?.joinToString("\n") ?: throw IllegalStateException("No response received from Aider process")


    override fun sendCommandAsync(command: String): Flux<String> {
        return processManager.sendCommandAsync(command)
    }

    override fun isReadyForCommand(): Boolean {
        return processManager.isReadyForCommand()
    }
}

interface AiderProcessInteractor {

    fun sendCommandSync(command: String): String
    fun sendCommandAsync(command: String): Flux<String>
    fun isReadyForCommand(): Boolean
}
