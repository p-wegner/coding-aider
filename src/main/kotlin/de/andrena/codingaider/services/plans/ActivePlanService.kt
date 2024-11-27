package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ActivePlanService(private val project: Project) {
    private var activePlan: AiderPlan? = null

    fun setActivePlan(plan: AiderPlan) {
        activePlan = plan
    }

    fun getActivePlan(): AiderPlan? = activePlan

    fun clearActivePlan() {
        activePlan = null
    }

    fun continuePlan() {
        activePlan?.let { plan ->
            project.service<ContinuePlanService>().continuePlan(plan)
        }
    }
}
