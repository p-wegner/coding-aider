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

    @Test
    fun `should execute first subplan when root plan has incomplete subplans`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(plan)
        
        val nextExecutable = activePlanService.getNextExecutableSubplan()
        assertThat(nextExecutable).isEqualTo(plan.childPlans[0])
        assertThat(nextExecutable?.mainPlanFile?.filePath).endsWith("auth_subplan.md")
    }

    @Test
    fun `should transition to next subplan when current subplan completes`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        
        // Complete first subplan
        val completedFirstSubplan = plan.childPlans[0].copy(
            checklist = plan.childPlans[0].checklist.map { it.copy(checked = true) }
        )
        val updatedPlan = plan.copy(
            childPlans = listOf(completedFirstSubplan, plan.childPlans[1])
        )
        activePlanService.setActivePlan(updatedPlan)
        
        val nextExecutable = activePlanService.getNextExecutableSubplan()
        assertThat(nextExecutable).isEqualTo(updatedPlan.childPlans[1])
        assertThat(nextExecutable?.mainPlanFile?.filePath).endsWith("ui_subplan.md")
    }

    @Test
    fun `should include correct files for subplan execution`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(plan)
        
        val files = activePlanService.collectVirtualFilesForExecution()
        
        // Should include root plan files and first subplan files
        assertThat(files).isNotEmpty
        assertThat(files.any { it.filePath.contains("root_plan") }).isTrue
        assertThat(files.any { it.filePath.contains("auth_subplan") }).isTrue
        assertThat(files.none { it.filePath.contains("ui_subplan") }).isTrue
    }

    @Test
    fun `should include correct files for root plan execution`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        
        // Complete all subplans
        val completedSubplans = plan.childPlans.map { subplan ->
            subplan.copy(
                checklist = subplan.checklist.map { it.copy(checked = true) }
            )
        }
        val completedPlan = plan.copy(childPlans = completedSubplans)
        activePlanService.setActivePlan(completedPlan)
        
        val files = activePlanService.collectVirtualFilesForExecution()
        
        // Should include all plan files when executing root plan
        assertThat(files).isNotEmpty
        assertThat(files.any { it.filePath.contains("root_plan") }).isTrue
    }

    @Test
    fun `should handle subplan state refresh correctly`() {
        val plan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(plan)
        
        // Get initial state
        val initialSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(initialSubplan).isEqualTo(plan.childPlans[0])
        
        // Refresh state (simulating plan reload)
        activePlanService.refreshPlanState()
        
        // Should maintain the same execution target
        val refreshedSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(refreshedSubplan?.mainPlanFile?.filePath).isEqualTo(initialSubplan?.mainPlanFile?.filePath)
    }
}
