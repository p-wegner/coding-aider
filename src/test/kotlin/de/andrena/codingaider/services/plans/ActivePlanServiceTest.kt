package de.andrena.codingaider.services.plans

import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.mockito.Mockito.mock

class ActivePlanServiceTest : LightPlatformTestCase() {

    private lateinit var activePlanService: ActivePlanService
    private lateinit var aiderPlanService: AiderPlanService

    override fun setUp() {
        super.setUp()
        aiderPlanService = mock(AiderPlanService::class.java)
        activePlanService = ActivePlanService(project)
    }

    @Test
    fun testSetAndGetActivePlan() {
        val mockPlan = AiderPlan(
            plan = "Test Plan",
            checklist = listOf(ChecklistItem("Test Item", false, emptyList())),
            planFiles = emptyList(),
            contextFiles = emptyList()
        )

        activePlanService.setActivePlan(mockPlan)
        assertEquals(mockPlan, activePlanService.getActivePlan())
    }

    @Test
    fun testClearActivePlan() {
        val mockPlan = AiderPlan(
            plan = "Test Plan",
            checklist = listOf(ChecklistItem("Test Item", false, emptyList())),
            planFiles = emptyList(),
            contextFiles = emptyList()
        )

        activePlanService.setActivePlan(mockPlan)
        activePlanService.clearActivePlan()
        assertNull(activePlanService.getActivePlan())
    }

}
