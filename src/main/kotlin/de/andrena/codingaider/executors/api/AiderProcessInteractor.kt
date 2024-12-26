package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.sidecar.AiderProcessManager
import reactor.core.publisher.Flux

class DefaultAiderProcessInteractor(private val project: Project) : AiderProcessInteractor {
    private val processManager = project.service<AiderProcessManager>()

    override fun sendCommandSync(command: String, planId: String?): String =
        processManager.sendCommandAsync(command,planId)
            .collectList()
            .block()
            ?.joinToString("\n") ?: throw IllegalStateException("No response received from Aider process")


    override fun sendCommandAsync(command: String, planId: String?): Flux<String> {
        return processManager.sendCommandAsync(command,planId)
    }

    override fun isReadyForCommand(planId: String?): Boolean {
        return processManager.isReadyForCommand(planId)
    }
}

interface AiderProcessInteractor {

    fun sendCommandSync(command: String, planId: String?=null): String
    fun sendCommandAsync(command: String, planId: String?=null): Flux<String>
    fun isReadyForCommand(planId: String?=null): Boolean
}
