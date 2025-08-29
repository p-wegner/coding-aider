package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.TestPlanFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
@Disabled("proper service mocking required")
class SubplanPromptServiceTest {

    private lateinit var project: Project
    private lateinit var subplanPromptService: AiderPlanPromptService
    private lateinit var aiderSettings: AiderSettings

    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        aiderSettings = mock(AiderSettings::class.java)
        subplanPromptService = AiderPlanPromptService(project)
        `when`(project.service<AiderSettings>()).thenReturn(aiderSettings)
    }

    @Test
    fun `should create subplan execution prompt with context`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        val currentSubplan = rootPlan.childPlans[0]
        val commandData = CommandData(
            message = "",
            useYesFlag = false,
            llm = "",
            additionalArgs = "",
            files = emptyList(),
            lintCmd = "",
            projectPath = "",
            aiderMode = AiderMode.STRUCTURED,
            sidecarMode = false,
            planId = ""
        )

        `when`(aiderSettings.useSingleFilePlanMode).thenReturn(false)

        val prompt = subplanPromptService.createSubplanExecutionPrompt(rootPlan, currentSubplan, commandData)

        assertThat(prompt).contains("SUBPLAN EXECUTION CONTEXT")
        assertThat(prompt).contains("Root Plan: root_plan_with_subplans")
        assertThat(prompt).contains("Current Subplan: auth_subplan")
        assertThat(prompt).contains("Completed Subplans: 0/2")
        assertThat(prompt).contains("Remaining Subplans: ui_subplan")
    }

    @Test
    fun `should include subplan progress information`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        
        // Complete first subplan
        val completedFirstSubplan = rootPlan.childPlans[0].copy(
            checklist = rootPlan.childPlans[0].checklist.map { it.copy(checked = true) }
        )
        val updatedRootPlan = rootPlan.copy(
            childPlans = listOf(completedFirstSubplan, rootPlan.childPlans[1])
        )
        val currentSubplan = updatedRootPlan.childPlans[1]
        
        val commandData = CommandData(
            message = "",
            useYesFlag = false,
            llm = "",
            additionalArgs = "",
            files = emptyList(),
            lintCmd = "",
            projectPath = "",
            aiderMode = AiderMode.STRUCTURED,
            sidecarMode = false,
            planId = ""
        )

        `when`(aiderSettings.useSingleFilePlanMode).thenReturn(false)

        val prompt = subplanPromptService.createSubplanExecutionPrompt(updatedRootPlan, currentSubplan, commandData)

        assertThat(prompt).contains("Completed Subplans: 1/2")
        assertThat(prompt).contains("Already Completed Subplans: auth_subplan")
        assertThat(prompt).contains("Current Subplan: ui_subplan")
    }

    @Test
    fun `should fall back to standard prompt for root plan`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        val commandData = CommandData(
            message = "Continue with root plan",
            useYesFlag = false,
            llm = "",
            additionalArgs = "",
            files = emptyList(),
            lintCmd = "",
            projectPath = "",
            aiderMode = AiderMode.STRUCTURED,
            sidecarMode = false,
            planId = ""
        )

        `when`(aiderSettings.useSingleFilePlanMode).thenReturn(false)

        val prompt = subplanPromptService.createAiderPlanSystemPrompt(commandData)

        // Should not contain subplan-specific context when executing root plan
        assertThat(prompt).doesNotContain("SUBPLAN EXECUTION CONTEXT")
        assertThat(prompt).contains("Continue with root plan")
    }

    @Test
    fun `should handle completed subplans in prompt`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        
        // Complete all subplans
        val completedSubplans = rootPlan.childPlans.map { subplan ->
            subplan.copy(
                checklist = subplan.checklist.map { it.copy(checked = true) }
            )
        }
        val completedRootPlan = rootPlan.copy(childPlans = completedSubplans)
        val currentSubplan = completedRootPlan.childPlans[0] // Even though complete, test the prompt generation
        
        val commandData = CommandData(
            message = "",
            useYesFlag = false,
            llm = "",
            additionalArgs = "",
            files = emptyList(),
            lintCmd = "",
            projectPath = "",
            aiderMode = AiderMode.STRUCTURED,
            sidecarMode = false,
            planId = ""
        )

        `when`(aiderSettings.useSingleFilePlanMode).thenReturn(false)

        val prompt = subplanPromptService.createSubplanExecutionPrompt(completedRootPlan, currentSubplan, commandData)

        assertThat(prompt).contains("Completed Subplans: 2/2")
        assertThat(prompt).contains("Already Completed Subplans: auth_subplan, ui_subplan")
        assertThat(prompt).doesNotContain("Remaining Subplans:")
    }
}
