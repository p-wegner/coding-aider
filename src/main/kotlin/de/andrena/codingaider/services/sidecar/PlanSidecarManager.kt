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
    
    fun getOrCreateProcessManager(plan: AiderPlan): AiderProcessManager {
        val planId = plan.mainPlanFile?.filePath ?: throw IllegalStateException("Plan has no main file")
        return processManagers.computeIfAbsent(planId) { 
            AiderProcessManager(project).also {
                logger.info("Created new process manager for plan: $planId")
            }
        }
    }
    
    fun getProcessManager(plan: AiderPlan): AiderProcessManager? {
        val planId = plan.mainPlanFile?.filePath ?: return null
        return processManagers[planId]
    }
    
    fun cleanupPlanProcess(plan: AiderPlan) {
        val planId = plan.mainPlanFile?.filePath ?: return
        processManagers.remove(planId)?.dispose()
        logger.info("Cleaned up process manager for plan: $planId")
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
