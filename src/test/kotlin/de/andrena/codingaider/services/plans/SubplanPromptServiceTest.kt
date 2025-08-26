package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.TestPlanFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

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
}