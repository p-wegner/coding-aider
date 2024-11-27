package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ContinuePlanService(private val project: Project) {
    fun continuePlan(selectedPlan: AiderPlan? = null) {
        if (selectedPlan != null) {
            project.service<ActivePlanService>().setActivePlan(selectedPlan)
        }
        project.service<ActivePlanService>().continuePlan()
    }
}
