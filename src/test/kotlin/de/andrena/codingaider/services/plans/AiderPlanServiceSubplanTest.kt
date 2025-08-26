package de.andrena.codingaider.services.plans

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.messages.MessageBus
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.model.ContextFileHandler
import de.andrena.codingaider.services.AiderIgnoreService
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.AiderSettings
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

class AiderPlanServiceSubplanTest {

    private lateinit var project: Project
    private lateinit var aiderPlanService: AiderPlanService
    private lateinit var plansDir: File
    private lateinit var aiderIgnoreService: AiderIgnoreService
    private lateinit var aiderProjectSettings: AiderProjectSettings
    private lateinit var planExecutionCostService: PlanExecutionCostService
    private lateinit var aiderSettings: AiderSettings
    private lateinit var aiderPlanPromptTemplates: AiderPlanPromptTemplates
    private lateinit var contextFileHandler: ContextFileHandler
    private lateinit var messageBus: MessageBus
    private lateinit var localFileSystem: LocalFileSystem
    private lateinit var aiderPlanPromptService: AiderPlanPromptService

    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        `when`(project.basePath).thenReturn(File(".").absolutePath)
        aiderIgnoreService = AiderIgnoreService(project)
        aiderProjectSettings = mock(AiderProjectSettings::class.java)
        planExecutionCostService = mock(PlanExecutionCostService::class.java)
        aiderSettings = mock(AiderSettings::class.java)
        aiderPlanPromptTemplates = mock(AiderPlanPromptTemplates::class.java)
        contextFileHandler = mock(ContextFileHandler::class.java)
        messageBus = mock(MessageBus::class.java)
        localFileSystem = mock(LocalFileSystem::class.java)
        aiderPlanPromptService = mock(AiderPlanPromptService::class.java)

        `when`(project.service<AiderPlanPromptService>()).thenReturn(aiderPlanPromptService)
        `when`(project.service<ActivePlanService>()).thenReturn(ActivePlanService(project))
        `when`(project.service<AiderProjectSettings>()).thenReturn(aiderProjectSettings)
        `when`(project.service<PlanExecutionCostService>()).thenReturn(planExecutionCostService)
        `when`(project.service<AiderSettings>()).thenReturn(aiderSettings)
        `when`(project.messageBus).thenReturn(messageBus)
        `when`(messageBus.syncPublisher(PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC)).thenReturn(mock(PersistentFilesChangedTopic::class.java))
        `when`(ApplicationManager.getApplication()).thenReturn(mock(com.intellij.openapi.application.Application::class.java))
        `when`(LocalFileSystem.getInstance()).thenReturn(localFileSystem)
        `when`(AiderProjectSettings.getInstance(project)).thenReturn(aiderProjectSettings)

        aiderPlanService = AiderPlanService(project, aiderIgnoreService)
        plansDir = File("src/test/resources/plans/multi_file_plans")
    }

    @Test
    fun `should parse multi-file subplans correctly`() {
        val rootPlanFile = File(plansDir, "root_plan_with_subplans.md")

        val rootPlan = aiderPlanService.loadPlanFromFile(rootPlanFile)

        assertThat(rootPlan).isNotNull
        assertThat(rootPlan!!.childPlans).hasSize(2)

        val authSubplan = rootPlan.childPlans[0]
        assertThat(authSubplan.plan).contains("Authentication System Implementation")
        assertThat(authSubplan.mainPlanFile?.filePath).endsWith("auth_subplan.md")
        assertThat(authSubplan.parentPlan).isEqualTo(rootPlan)

        val uiSubplan = rootPlan.childPlans[1]
        assertThat(uiSubplan.plan).contains("UI Components Implementation")
        assertThat(uiSubplan.mainPlanFile?.filePath).endsWith("ui_subplan.md")
        assertThat(uiSubplan.parentPlan).isEqualTo(rootPlan)
    }
}
