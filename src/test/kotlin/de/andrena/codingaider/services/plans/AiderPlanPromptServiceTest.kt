package de.andrena.codingaider.services.plans

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.registerServiceInstance
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AiderPlanPromptServiceTest : BasePlatformTestCase() {

    private lateinit var planPromptService: AiderPlanPromptService
    private lateinit var myProjectFixture: IdeaProjectTestFixture
    private val myProject: Project get() = myProjectFixture.project
    private lateinit var mockSettings: AiderSettings
    private lateinit var commandData: CommandData
    private val projectPath = "/project"

    @BeforeEach
    fun mySetup() {
        myProjectFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getTestName(false)).fixture
        myProjectFixture.setUp()
        
        // Mock settings as application service
        mockSettings = mock()
        val application = ApplicationManager.getApplication()
        application.registerServiceInstance(AiderSettings::class.java, mockSettings)
        
        // Initialize service
        planPromptService = AiderPlanPromptService(myProject)
        
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
}
