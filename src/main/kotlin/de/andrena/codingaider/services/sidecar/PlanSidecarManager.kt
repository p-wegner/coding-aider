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
    
    fun cleanupPlanProcess(plan: AiderPlan) {
        val planId = plan.mainPlanFile?.filePath ?: return
        try {
            notifyProcessState(planId, ProcessState.DISPOSED)
            processManagers.remove(planId)?.dispose()
            logger.info("Cleaned up process manager for plan: $planId")
        } catch (e: Exception) {
            notifyProcessError(planId, e)
            throw e
        }
    }
    
    fun cleanupAllProcesses() {
        processManagers.values.forEach { it.dispose() }
        processManagers.clear()
        logger.info("Cleaned up all plan process managers")
    }

    override fun dispose() {
        cleanupAllProcesses()
    }
}
