package de.andrena.codingaider.providers

import com.intellij.openapi.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Interface for managing AI provider processes in sidecar mode
 */
interface AIProcessManager : Disposable {
    val provider: AIProvider
    
    /**
     * Starts a process for the AI provider
     * @param command The command to execute
     * @param workingDir Working directory for the process
     * @param verbose Enable verbose logging
     * @param planId Optional plan ID for plan-specific processes
     * @return Mono that completes when the process is ready
     */
    fun startProcess(
        command: List<String>,
        workingDir: String,
        verbose: Boolean = false,
        planId: String? = null
    ): Mono<Void>
    
    /**
     * Sends a command to the running process asynchronously
     * @param command The command to send
     * @param planId Optional plan ID for plan-specific processes
     * @return Flux of response strings from the process
     */
    fun sendCommandAsync(command: String, planId: String? = null): Flux<String>
    
    /**
     * Checks if the process is ready to receive commands
     * @param planId Optional plan ID for plan-specific processes
     * @return true if ready, false otherwise
     */
    fun isReadyForCommand(planId: String? = null): Boolean
    
    /**
     * Interrupts the current command execution
     * @param planId Optional plan ID for plan-specific processes
     */
    fun interruptCurrentCommand(planId: String? = null)
    
    /**
     * Stops the process
     * @param planId Optional plan ID for plan-specific processes
     */
    fun stopProcess(planId: String? = null)
    
    /**
     * Gets the display name for this process manager
     */
    fun getDisplayName(): String
}