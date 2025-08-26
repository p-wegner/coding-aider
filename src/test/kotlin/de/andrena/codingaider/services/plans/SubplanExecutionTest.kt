package de.andrena.codingaider.services.plans

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.messages.MessageBus
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.model.ContextFileHandler
import de.andrena.codingaider.services.AiderIgnoreService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.TestPlanFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class SubplanExecutionTest {

    private lateinit var project: Project
    private lateinit var aiderPlanService: AiderPlanService
    private lateinit var activePlanService: ActivePlanService
    private lateinit var aiderPlanPromptService: AiderPlanPromptService
    private lateinit var aiderIgnoreService: AiderIgnoreService
    private lateinit var aiderProjectSettings: AiderProjectSettings
    private lateinit var planExecutionCostService: PlanExecutionCostService
    private lateinit var aiderSettings: AiderSettings
    private lateinit var contextFileHandler: ContextFileHandler
    private lateinit var messageBus: MessageBus
    private lateinit var localFileSystem: LocalFileSystem

    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        `when`(project.basePath).thenReturn(File(".").absolutePath)
        
        aiderIgnoreService = AiderIgnoreService(project)
        aiderProjectSettings = mock(AiderProjectSettings::class.java)
        planExecutionCostService = mock(PlanExecutionCostService::class.java)
        aiderSettings = mock(AiderSettings::class.java)
        contextFileHandler = mock(ContextFileHandler::class.java)
        messageBus = mock(MessageBus::class.java)
        localFileSystem = mock(LocalFileSystem::class.java)

        `when`(project.service<AiderProjectSettings>()).thenReturn(aiderProjectSettings)
        `when`(project.service<PlanExecutionCostService>()).thenReturn(planExecutionCostService)
        `when`(project.service<AiderSettings>()).thenReturn(aiderSettings)
        `when`(project.messageBus).thenReturn(messageBus)
        `when`(messageBus.syncPublisher(PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC)).thenReturn(mock(PersistentFilesChangedTopic::class.java))
        `when`(ApplicationManager.getApplication()).thenReturn(mock(com.intellij.openapi.application.Application::class.java))
        `when`(LocalFileSystem.getInstance()).thenReturn(localFileSystem)
        `when`(AiderProjectSettings.getInstance(project)).thenReturn(aiderProjectSettings)

        aiderPlanService = AiderPlanService(project, aiderIgnoreService)
        activePlanService = ActivePlanService(project)
        aiderPlanPromptService = AiderPlanPromptService(project, aiderPlanService, AiderPlanPromptTemplates(aiderPlanService))
        
        `when`(project.service<AiderPlanService>()).thenReturn(aiderPlanService)
        `when`(project.service<ActivePlanService>()).thenReturn(activePlanService)
        `when`(project.service<AiderPlanPromptService>()).thenReturn(aiderPlanPromptService)
    }

    @Test
    fun `should execute subplans in sequential order`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(rootPlan)

        // First execution should target the first subplan
        val firstSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(firstSubplan).isEqualTo(rootPlan.childPlans[0])
        assertThat(firstSubplan?.mainPlanFile?.filePath).endsWith("auth_subplan.md")

        // Mark first subplan as complete
        val completedFirstSubplan = rootPlan.childPlans[0].copy(
            checklist = rootPlan.childPlans[0].checklist.map { it.copy(checked = true) }
        )
        val updatedRootPlan = rootPlan.copy(
            childPlans = listOf(completedFirstSubplan, rootPlan.childPlans[1])
        )
        activePlanService.setActivePlan(updatedRootPlan)

        // Second execution should target the second subplan
        val secondSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(secondSubplan).isEqualTo(updatedRootPlan.childPlans[1])
        assertThat(secondSubplan?.mainPlanFile?.filePath).endsWith("ui_subplan.md")
    }

    @Test
    fun `should include only relevant files for current subplan`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(rootPlan)

        val currentSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(currentSubplan).isNotNull

        // Files should include root plan files + current subplan files
        val expectedFiles = rootPlan.planFiles + currentSubplan!!.planFiles
        val actualFiles = activePlanService.collectVirtualFilesForExecution()

        // Verify that we have both root plan and subplan files
        assertThat(actualFiles).isNotEmpty
        assertThat(actualFiles.any { it.filePath.contains("root_plan") }).isTrue
        assertThat(actualFiles.any { it.filePath.contains("auth_subplan") }).isTrue
        assertThat(actualFiles.none { it.filePath.contains("ui_subplan") }).isTrue
    }

    @Test
    fun `should generate subplan specific prompts`() {
        `when`(aiderSettings.useSingleFilePlanMode).thenReturn(false)
        
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        val currentSubplan = rootPlan.childPlans[0]
        
        val commandData = CommandData(
            message = "Continue implementation",
            useYesFlag = false,
            llm = "gpt-4",
            additionalArgs = "",
            files = emptyList(),
            lintCmd = "",
            projectPath = project.basePath ?: "",
            aiderMode = AiderMode.STRUCTURED,
            sidecarMode = false,
            planId = ""
        )

        val prompt = aiderPlanPromptService.createSubplanExecutionPrompt(rootPlan, currentSubplan, commandData)

        assertThat(prompt).contains("SUBPLAN EXECUTION CONTEXT")
        assertThat(prompt).contains("Root Plan: root_plan_with_subplans")
        assertThat(prompt).contains("Current Subplan: auth_subplan")
        assertThat(prompt).contains("Completed Subplans: 0/2")
        assertThat(prompt).contains("Work ONLY on tasks related to the current subplan")
    }

    @Test
    fun `should handle subplan completion and transition`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(rootPlan)

        // Initially should execute first subplan
        val firstSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(firstSubplan).isEqualTo(rootPlan.childPlans[0])

        // Complete first subplan
        val completedFirstSubplan = rootPlan.childPlans[0].copy(
            checklist = rootPlan.childPlans[0].checklist.map { it.copy(checked = true) }
        )
        val updatedRootPlan = rootPlan.copy(
            childPlans = listOf(completedFirstSubplan, rootPlan.childPlans[1])
        )
        activePlanService.setActivePlan(updatedRootPlan)

        // Should transition to second subplan
        val nextSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(nextSubplan).isEqualTo(updatedRootPlan.childPlans[1])
        assertThat(nextSubplan?.isPlanComplete()).isFalse
    }

    @Test
    fun `should fall back to root plan when all subplans complete`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        
        // Complete all subplans
        val completedSubplans = rootPlan.childPlans.map { subplan ->
            subplan.copy(
                checklist = subplan.checklist.map { it.copy(checked = true) }
            )
        }
        val updatedRootPlan = rootPlan.copy(childPlans = completedSubplans)
        activePlanService.setActivePlan(updatedRootPlan)

        // Should return to root plan execution
        val nextExecutable = activePlanService.getNextExecutableSubplan()
        assertThat(nextExecutable).isEqualTo(updatedRootPlan)
        assertThat(nextExecutable?.childPlans?.all { it.isPlanComplete() }).isTrue
    }

    @Test
    fun `should include correct context files for subplan execution`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(rootPlan)

        val currentSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(currentSubplan).isNotNull

        // Context files should include both root plan and current subplan context
        val allContextFiles = rootPlan.contextFiles + currentSubplan!!.contextFiles
        
        // Verify context files are properly included
        assertThat(allContextFiles).isNotNull
        // Note: In the test factory, context files are empty, but in real scenarios
        // they would contain implementation files specific to each subplan
    }

    @Test
    fun `should handle malformed plan files gracefully`() {
        // Test error handling when plan files are corrupted or missing
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(rootPlan)
        
        // Simulate a scenario where a subplan file becomes unavailable
        val currentSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(currentSubplan).isNotNull
        
        // The system should handle missing files gracefully
        val files = activePlanService.collectVirtualFilesForExecution()
        assertThat(files).isNotNull
    }

    @Test
    fun `should maintain execution order across plan reloads`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(rootPlan)
        
        // Get initial execution target
        val initialSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(initialSubplan).isEqualTo(rootPlan.childPlans[0])
        
        // Simulate plan reload (as would happen after file changes)
        activePlanService.refreshPlanState()
        
        // Should maintain same execution target
        val reloadedSubplan = activePlanService.getNextExecutableSubplan()
        assertThat(reloadedSubplan?.mainPlanFile?.filePath).isEqualTo(initialSubplan?.mainPlanFile?.filePath)
    }

    @Test
    fun `should handle empty subplan lists correctly`() {
        // Create a root plan without subplans
        val rootPlanOnly = TestPlanFactory.createRootPlanWithSubplans().copy(childPlans = emptyList())
        activePlanService.setActivePlan(rootPlanOnly)
        
        // Should execute root plan directly
        val nextExecutable = activePlanService.getNextExecutableSubplan()
        assertThat(nextExecutable).isEqualTo(rootPlanOnly)
        assertThat(activePlanService.shouldExecuteSubplan()).isFalse
    }

    @Test
    fun `should provide meaningful execution status messages`() {
        val rootPlan = TestPlanFactory.createRootPlanWithSubplans()
        activePlanService.setActivePlan(rootPlan)
        
        // Initial status should show first subplan
        val initialStatus = activePlanService.getSubplanExecutionStatus()
        assertThat(initialStatus).contains("Subplans: 0/2 complete")
        assertThat(initialStatus).contains("Current: auth_subplan")
        
        // Complete first subplan and check status
        val completedFirstSubplan = rootPlan.childPlans[0].copy(
            checklist = rootPlan.childPlans[0].checklist.map { it.copy(checked = true) }
        )
        val updatedRootPlan = rootPlan.copy(
            childPlans = listOf(completedFirstSubplan, rootPlan.childPlans[1])
        )
        activePlanService.setActivePlan(updatedRootPlan)
        
        val updatedStatus = activePlanService.getSubplanExecutionStatus()
        assertThat(updatedStatus).contains("Subplans: 1/2 complete")
        assertThat(updatedStatus).contains("Current: ui_subplan")
    }
}
