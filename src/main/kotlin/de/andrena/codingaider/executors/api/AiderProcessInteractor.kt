package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.AiderOutputParser
import de.andrena.codingaider.services.AiderProcessManager

class DefaultAiderProcessInteractor(private val project: Project) : AiderProcessInteractor {
    private val processManager = project.service<AiderProcessManager>()

    override fun sendCommand(command: String): String {
        return processManager.sendCommand(command)
    }

    override fun parseOutput(output: String): AiderOutputState {
        return AiderOutputParser.parseOutput(output)
    }

    override fun isReadyForCommand(): Boolean {
        // TODO: Implement actual readiness check
        return true
    }
}

/**
 * Represents the current state of Aider's output
 */
data class AiderOutputState(
    val isPrompting: Boolean = false,
    val hasError: Boolean = false,
    val isWaitingForConfirmation: Boolean = false,
    val message: String = ""
)
