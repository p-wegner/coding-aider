package de.andrena.codingaider.services.plans

import com.intellij.openapi.project.Project
import de.andrena.codingaider.utils.TestPlanFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class ActivePlanServiceTest {

    private lateinit var project: Project
    private lateinit var activePlanService: ActivePlanService

    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        activePlanService = ActivePlanService(project)
    }

    @Test
    fun `should set and get active plan`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(plan)
        assertThat(activePlanService.getActivePlan()).isEqualTo(plan)
    }

    @Test
    fun `should clear active plan`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(plan)
        activePlanService.clearActivePlan()
        assertThat(activePlanService.getActivePlan()).isNull()
    }

    @Test
    fun `should get next executable subplan`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(plan)
        val nextSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(nextSubplan).isEqualTo(plan.childPlans[0])
    }

    @Test
    fun `should return root plan when all subplans are complete`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        val completedPlan = plan.copy(
            childPlans = plan.childPlans.map { subplan ->
                subplan.copy(
                    checklist = subplan.checklist.map { item ->
                        item.copy(checked = true)
                    }
                )
            }
        )
        activePlanService.setActivePlan(completedPlan)
        val nextSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(nextSubplan).isEqualTo(completedPlan)
    }
}
