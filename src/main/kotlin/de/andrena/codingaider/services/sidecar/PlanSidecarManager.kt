package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.services.plans.AiderPlan
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class PlanSidecarManager(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(PlanSidecarManager::class.java)
    private val processManagers = ConcurrentHashMap<String, AiderProcessManager>()
    private val processStateListeners = mutableListOf<(String, ProcessState) -> Unit>()
    private val processErrorListeners = mutableListOf<(String, Throwable) -> Unit>()

    enum class ProcessState {
        STARTING, READY, FAILED, DISPOSED, RECOVERING
    }
    
    fun addProcessStateListener(listener: (String, ProcessState) -> Unit) {
        processStateListeners.add(listener)
    }

    fun removeProcessStateListener(listener: (String, ProcessState) -> Unit) {
        processStateListeners.remove(listener)
    }

    fun addProcessErrorListener(listener: (String, Throwable) -> Unit) {
        processErrorListeners.add(listener)
    }

    fun removeProcessErrorListener(listener: (String, Throwable) -> Unit) {
        processErrorListeners.remove(listener)
    }

    private fun notifyProcessState(planId: String, state: ProcessState) {
        processStateListeners.forEach { it(planId, state) }
    }

    private fun notifyProcessError(planId: String, error: Throwable) {
        processErrorListeners.forEach { it(planId, error) }
    }

    fun getOrCreateProcessManager(plan: AiderPlan): AiderProcessManager {
        val planId = plan.mainPlanFile?.filePath ?: throw IllegalStateException("Plan has no main file")
        return processManagers.computeIfAbsent(planId) { 
            try {
                notifyProcessState(planId, ProcessState.STARTING)
                AiderProcessManager(project).also {
                    logger.info("Created new process manager for plan: $planId")
                    notifyProcessState(planId, ProcessState.READY)
                }
            } catch (e: Exception) {
                notifyProcessState(planId, ProcessState.FAILED)
                notifyProcessError(planId, e)
                throw e
            }
        }
    }
    
    fun getProcessManager(plan: AiderPlan): AiderProcessManager? {
        val planId = plan.mainPlanFile?.filePath ?: return null
        return processManagers[planId]
    }
    
    fun cleanupPlanProcess(plan: AiderPlan, force: Boolean = false) {
        val planId = plan.mainPlanFile?.filePath ?: return
        try {
            // Check if process should be cleaned up
            if (!force && isProcessActive(planId)) {
                logger.info("Process for plan $planId is still active, skipping cleanup")
                return
            }

            notifyProcessState(planId, ProcessState.DISPOSED)
            processManagers.remove(planId)?.dispose()
            logger.info("Successfully disposed Aider sidecar process for plan $planId")
        } catch (e: Exception) {
            notifyProcessError(planId, e)
            throw e
        }
    }

    private fun isProcessActive(planId: String): Boolean {
        val processManager = processManagers[planId] ?: return false
        return processManager.isReadyForCommand(planId)
    }

    // Cleanup inactive processes periodically
    private val cleanupJob = java.util.Timer("PlanProcessCleanup").apply {
        scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                try {
                    cleanupInactiveProcesses()
                } catch (e: Exception) {
                    logger.error("Error during process cleanup", e)
                }
            }
        }, CLEANUP_INTERVAL, CLEANUP_INTERVAL)
    }

    private fun cleanupInactiveProcesses() {
        val inactiveTimeout = System.currentTimeMillis() - PROCESS_INACTIVE_TIMEOUT
        processManagers.entries.removeIf { (planId, manager) ->
            try {
                if (!manager.isReadyForCommand(planId)) {
                    logger.info("Cleaning up inactive process for plan: $planId")
                    manager.dispose()
                    true
                } else false
            } catch (e: Exception) {
                logger.error("Error checking process state for $planId", e)
                true
            }
        }
    }

    companion object {
        private const val CLEANUP_INTERVAL = 5 * 60 * 1000L // 5 minutes
        private const val PROCESS_INACTIVE_TIMEOUT = 30 * 60 * 1000L // 30 minutes
    }
    
    fun cleanupAllProcesses() {
        processManagers.values.forEach { it.dispose() }
        processManagers.clear()
        logger.info("Cleaned up all plan process managers")
    }

    override fun dispose() {
        cleanupJob.cancel()
        cleanupAllProcesses()
    }
}
