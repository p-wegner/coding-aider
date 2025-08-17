package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ContinuePlanService(private val project: Project) {
    fun continuePlan(selectedPlan: AiderPlan? = null) {
        val activePlanService = project.service<ActivePlanService>()
        if (selectedPlan != null) {
            activePlanService.setActivePlan(selectedPlan)
        }
        activePlanService.continuePlan()
    }
}
