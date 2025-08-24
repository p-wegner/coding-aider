package de.andrena.codingaider.services.plans

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@Disabled("service mocking is wrong, needs to be fixed")
class AiderPlanPromptServiceTest {

    private lateinit var planPromptService: AiderPlanPromptService
    private lateinit var mockSettings: AiderSettings
    private lateinit var mockProject: Project
    private lateinit var commandData: CommandData
    private val projectPath = "/project"

    @BeforeEach
    fun setUp() {
        mockProject = mock()
        mockSettings = mock()
        whenever(mockProject.getService(AiderProjectSettings::class.java)).thenReturn(mock())
        whenever(mockProject.getService(AiderSettings::class.java)).thenReturn(mockSettings)
        whenever(mockProject.getService(AiderPlanService::class.java)).thenReturn(AiderPlanService(mockProject))
        whenever(mockProject.getService(AiderPlanPromptService::class.java)).thenReturn(
            AiderPlanPromptService(
                mockProject
            )
        )
        // Initialize service
        planPromptService = AiderPlanPromptService(mockProject)
        
        // Default command data
        commandData = CommandData(
            projectPath = projectPath,
            files = listOf(FileData("$projectPath/src/main/kotlin/SomeFile.kt", false)),
            message = "Test message",
            aiderMode = AiderMode.STRUCTURED,
            llm = "--4o",
            useYesFlag = true,
            editFormat = "diff",
            additionalArgs = "",
            lintCmd = "",
            deactivateRepoMap = false,
            sidecarMode = false
        )
    }

    @Test
    fun `createAiderPlanSystemPrompt creates prompt for new plan when no plan files exist`() {
        // Given
        whenever(mockSettings.enableSubplans).thenReturn(true)
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(false)
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).contains("<SystemPrompt>")
        assertThat(result).contains("</SystemPrompt>")
        assertThat(result).contains("<UserPrompt> Test message </UserPrompt>")
        assertThat(result).contains("No plan exists yet")
        assertThat(result).contains("Create subplans only if necessary")
    }

    @Test
    fun `createAiderPlanSystemPrompt creates prompt for new plan with subplans disabled`() {
        // Given
        whenever(mockSettings.enableSubplans).thenReturn(false)
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(false)
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).contains("<SystemPrompt>")
        assertThat(result).contains("</SystemPrompt>")
        assertThat(result).contains("No plan exists yet")
        assertThat(result).doesNotContain("Create subplans only if necessary")
    }

    @Test
    fun `createAiderPlanSystemPrompt creates prompt for existing plan`() {
        // Given
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(false)
        val planFile = FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/existing_plan.md", false)
        commandData = commandData.copy(
            files = commandData.files + planFile
        )
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).contains("<SystemPrompt>")
        assertThat(result).contains("</SystemPrompt>")
        assertThat(result).contains("A plan already exists")
        assertThat(result).contains("Continue implementing the existing plan")
        assertThat(result).contains("/${AiderPlanService.AIDER_PLANS_FOLDER}/existing_plan.md")
    }

    @Test
    fun `createAiderPlanSystemPrompt handles slash commands`() {
        // Given
        commandData = commandData.copy(
            message = "/command Test message with command"
        )
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).startsWith("/command <SystemPrompt>")
        assertThat(result).contains("</SystemPrompt>")
        assertThat(result).contains("<UserPrompt> Test message with command </UserPrompt>")
    }

    @Test
    fun `createAiderPlanSystemPrompt handles slash commands without additional message`() {
        // Given
        commandData = commandData.copy(
            message = "/command"
        )
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).startsWith("/command <SystemPrompt>")
        assertThat(result).contains("</SystemPrompt>")
        assertThat(result).contains("<UserPrompt>  </UserPrompt>")
    }

    @Test
    fun `createPlanRefinementPrompt creates prompt with subplans enabled`() {
        // Given
        whenever(mockSettings.enableSubplans).thenReturn(true)
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(false)
        val plan = AiderPlan(
            plan = "test_plan.md",
            checklist = emptyList(),
            planFiles = emptyList(),
            contextFiles = emptyList()
        )
        val refinementRequest = "Add new feature"
        
        // When
        val result = planPromptService.createPlanRefinementPrompt(plan, refinementRequest)
        
        // Then
        assertThat(result).contains("<SystemPrompt>")
        assertThat(result).contains("You are refining an existing plan test_plan.md")
        assertThat(result).contains("Consider whether to use subplans for complex parts")
        assertThat(result).contains("<UserPrompt>Add new feature</UserPrompt>")
    }

    @Test
    fun `createPlanRefinementPrompt creates prompt with subplans disabled`() {
        // Given
        whenever(mockSettings.enableSubplans).thenReturn(false)
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(false)
        val plan = AiderPlan(
            plan = "test_plan.md",
            checklist = emptyList(),
            planFiles = emptyList(),
            contextFiles = emptyList()
        )
        val refinementRequest = "Add new feature"
        
        // When
        val result = planPromptService.createPlanRefinementPrompt(plan, refinementRequest)
        
        // Then
        assertThat(result).contains("<SystemPrompt>")
        assertThat(result).contains("You are refining an existing plan test_plan.md")
        assertThat(result).doesNotContain("Consider whether to use subplans for complex parts")
        assertThat(result).contains("<UserPrompt>Add new feature</UserPrompt>")
    }

    @Test
    fun `filterPlanRelevantFiles returns only plan-related files`() {
        // Given
        val files = listOf(
            FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan.md", false),
            FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan_checklist.md", false),
            FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan_context.yaml", false),
            FileData("$projectPath/${AiderPlanService.FINISHED_AIDER_PLANS_FOLDER}/finished_plan.md", false),
            FileData("$projectPath/src/main/kotlin/SomeFile.kt", false)
        )
        
        // When
        val result = planPromptService.filterPlanRelevantFiles(files)
        
        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.filePath }).containsExactlyInAnyOrder(
            "$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan.md",
            "$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan_checklist.md",
            "$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan_context.yaml"
        )
    }

    @Test
    fun `filterPlanMainFiles returns only main plan files`() {
        // Given
        val files = listOf(
            FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan.md", false),
            FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan_checklist.md", false),
            FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan_context.yaml", false),
            FileData("$projectPath/${AiderPlanService.FINISHED_AIDER_PLANS_FOLDER}/finished_plan.md", false),
            FileData("$projectPath/src/main/kotlin/SomeFile.kt", false)
        )
        
        // When
        val result = planPromptService.filterPlanMainFiles(files)
        
        // Then
        assertThat(result).hasSize(1)
        assertThat(result.map { it.filePath }).containsExactly(
            "$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/plan.md"
        )
    }

    // TODO: Fix these tests - the service dependencies aren't properly mocked
    /*
    @Test
    fun `createAiderPlanSystemPrompt uses single-file format when setting enabled`() {
        // Given
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(true)
        whenever(mockSettings.enableSubplans).thenReturn(false)
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).contains("Single-file plans are located")
        assertThat(result).contains("All content in one file for simplified management")
        assertThat(result).contains("embedded checklist")
    }
    
    @Test
    fun `createAiderPlanSystemPrompt uses multi-file format when setting disabled`() {
        // Given
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(false)
        whenever(mockSettings.enableSubplans).thenReturn(false)
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).contains("Plan files are located")
        assertThat(result).contains("three files with consistent naming")
        assertThat(result).contains("_checklist.md, _context.yaml")
    }
    
    @Test
    fun `createAiderPlanSystemPrompt uses single-file continuation prompt for existing plan`() {
        // Given
        whenever(mockSettings.useSingleFilePlanMode).thenReturn(true)
        whenever(mockSettings.enableSubplans).thenReturn(false)
        commandData = commandData.copy(
            files = listOf(
                FileData("$projectPath/src/main/kotlin/SomeFile.kt", false),
                FileData("$projectPath/${AiderPlanService.AIDER_PLANS_FOLDER}/existing_plan.md", false)
            )
        )
        
        // When
        val result = planPromptService.createAiderPlanSystemPrompt(commandData)
        
        // Then
        assertThat(result).contains("Single-file plan already exists")
        assertThat(result).contains("embedded checklist")
        assertThat(result).contains("embedded context YAML")
    }
    */
}
