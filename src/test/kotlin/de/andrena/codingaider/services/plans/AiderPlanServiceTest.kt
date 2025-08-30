package de.andrena.codingaider.services.plans

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class AiderPlanServiceTest : BasePlatformTestCase() {

    private lateinit var aiderPlanService: AiderPlanService
    private lateinit var plansDir: File

    private val singleFilePlanContent = """
    # [Coding Aider Plan]
    # Implement User Authentication
    This plan covers the implementation of a user authentication feature.
    ---
    # [Coding Aider Plan - Checklist]
    - [ ] Create User model
    - [x] Set up database schema
    - [ ] Implement registration endpoint
    ---
    # [Coding Aider Plan - Context]
    files:
      - path: "src/main/kotlin/com/example/model/User.kt"
        readOnly: false
      - path: "src/main/kotlin/com/example/db/schema.sql"
        readOnly: true
    """.trimIndent()

    private val multiFilePlanContent = """
    # [Coding Aider Plan]
    # Implement Core Application Logic
    ## Overview
    This plan outlines the steps to implement the core application logic, including database setup, service layer creation, and API endpoint implementation.
    ## Problem Description
    The application currently lacks a functional backend. This plan addresses the need to build out the essential components for a working service.
    ## Goals
    -   Set up a database connection and schema.
    -   Create a service layer to handle business logic.
    -   Implement API endpoints for client interaction.
    ## Additional Notes
    -   This is a critical path for the application, so it needs to be done carefully.
    -   Consider using a lightweight framework for the API endpoints.
    ## References
    -   [Project Documentation](docs/project_overview.md)
    -   [Database Schema](docs/db_schema.md)
    """.trimIndent()

    private val multiFileChecklistContent = """
    # [Coding Aider Plan - Checklist]
    # Authentication and UI System Implementation Checklist
    ## Main Implementation Tasks
    - [ ] Complete authentication system implementation
    - [ ] Complete UI components implementation
    - [ ] Integrate authentication with UI components
    - [ ] Run end-to-end tests
    - [ ] Update documentation
    """.trimIndent()

    private val multiFileContextContent = """
    ---
    files:
      - path: "src/main/kotlin/de/andrena/codingaider/security/AuthService.kt"
        readOnly: false
      - path: "src/main/kotlin/de/andrena/codingaider/ui/components/Button.kt"
        readOnly: false
      - path: "src/main/kotlin/de/andrena/codingaider/ui/components/Input.kt"
        readOnly: false
      - path: "src/main/kotlin/de/andrena/codingaider/utils/Logger.kt"
        readOnly: false
    """.trimIndent()

    private val planWithMissingFilesContent = """
    # [Coding Aider Plan]
    # Plan with Missing Files
    This plan is designed to test how the system handles missing checklist and context files.
    """.trimIndent()

    override fun setUp() {
        super.setUp()
        aiderPlanService = project.getService(AiderPlanService::class.java)
        plansDir = File(myFixture.tempDirPath, "src/test/resources/plans")
        plansDir.mkdirs()
        File(myFixture.tempDirPath, "src/test/resources/plans/multi_file_plans").mkdirs()


        myFixture.addFileToProject("src/test/resources/plans/single_file_plan.md", singleFilePlanContent)
        myFixture.addFileToProject("src/test/resources/plans/multi_file_plans/root_plan.md", multiFilePlanContent)
        myFixture.addFileToProject("src/test/resources/plans/multi_file_plans/root_plan_checklist.md", multiFileChecklistContent)
        myFixture.addFileToProject("src/test/resources/plans/multi_file_plans/root_plan_context.yaml", multiFileContextContent)
        myFixture.addFileToProject("src/test/resources/plans/multi_file_plans/plan_with_missing_files.md", planWithMissingFilesContent)
    }

    @Test
    fun testShouldParseSingleFilePlanCorrectly() {
        val planFile = File(plansDir, "single_file_plan.md")
        val plan = aiderPlanService.loadPlanFromFile(planFile)

        assertThat(plan).isNotNull
        assertThat(plan!!.plan).contains("Implement User Authentication")
        assertThat(plan.checklist).hasSize(3)
        assertThat(plan.checklist[0].description).isEqualTo("Create User model")
        assertThat(plan.contextFiles).hasSize(2)
        assertThat(plan.contextFiles[0].filePath).isEqualTo("src/main/kotlin/com/example/model/User.kt")
    }

    @Test
    fun testShouldParseMultiFilePlanCorrectly() {
        val planFile = File(plansDir, "multi_file_plans/root_plan.md")
        val plan = aiderPlanService.loadPlanFromFile(planFile)

        assertThat(plan).isNotNull
        assertThat(plan!!.plan).contains("Implement Core Application Logic")
        assertThat(plan.checklist).hasSize(5)
        assertThat(plan.checklist[0].description).isEqualTo("Complete authentication system implementation")
        assertThat(plan.contextFiles).hasSize(4)
        assertThat(plan.contextFiles[0].filePath).isEqualTo("src/main/kotlin/de/andrena/codingaider/security/AuthService.kt")
    }

    @Test
    fun testShouldHandleMissingContextOrChecklistFilesGracefully() {
        val planFile = File(plansDir, "multi_file_plans/plan_with_missing_files.md")
        val plan = aiderPlanService.loadPlanFromFile(planFile)

        assertThat(plan).isNotNull
        assertThat(plan!!.checklist).isEmpty()
        assertThat(plan.contextFiles).isEmpty()
    }

    @Test
    fun testShouldCorrectlyIdentifyPlanCompletionStatus() {
        val planFile = File(plansDir, "single_file_plan.md")
        val plan = aiderPlanService.loadPlanFromFile(planFile)
        assertThat(plan!!.isPlanComplete()).isFalse

        val completedPlan = plan.copy(
            checklist = plan.checklist.map { it.copy(checked = true) }
        )
        assertThat(completedPlan.isPlanComplete()).isTrue
    }
}
