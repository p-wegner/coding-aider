package de.andrena.codingaider.services.plans

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AiderPlanPromptTemplatesTest {

    @Test
    fun `planFileStructurePrompt contains required sections`() {
        // Given
        val prompt = AiderPlanPromptTemplates.planFileStructurePrompt
        
        // When/Then
        assertThat(prompt).contains("feature_name.md")
        assertThat(prompt).contains("feature_name_checklist.md")
        assertThat(prompt).contains("feature_name_context.yaml")
        assertThat(prompt).contains("Overview")
        assertThat(prompt).contains("Problem Description")
        assertThat(prompt).contains("Goals")
        assertThat(prompt).contains("Additional Notes")
        assertThat(prompt).contains("References")
    }

    @Test
    fun `subplanGuidancePrompt contains required guidance`() {
        // Given
        val prompt = AiderPlanPromptTemplates.subplanGuidancePrompt
        
        // When/Then
        assertThat(prompt).contains("Subplan Requirements")
        assertThat(prompt).contains("Name format: mainplan_subfeature")
        assertThat(prompt).contains("<!-- SUBPLAN:mainplan_subfeature -->")
        assertThat(prompt).contains("<!-- END_SUBPLAN -->")
        assertThat(prompt).contains("- [ ] Complete subfeature implementation")
    }

    @Test
    fun `noSubplansGuidancePrompt contains required guidance`() {
        // Given
        val prompt = AiderPlanPromptTemplates.noSubplansGuidancePrompt
        
        // When/Then
        assertThat(prompt).contains("Create a detailed checklist with atomic tasks")
        assertThat(prompt).contains("focus on clear, sequential implementation steps")
        assertThat(prompt).doesNotContain("without subplans")
        assertThat(prompt).doesNotContain("single comprehensive plan")
    }

    @Test
    fun `planFileFormatPrompt contains required format instructions`() {
        // Given
        val prompt = AiderPlanPromptTemplates.planFileFormatPrompt
        
        // When/Then
        assertThat(prompt).contains("Plan files are located in")
        assertThat(prompt).contains("Start plans with")
        assertThat(prompt).contains("Start checklists with")
        assertThat(prompt).contains("Checklist items should be atomic")
        assertThat(prompt).contains("Use consistent naming")
        assertThat(prompt).contains("Content Guidelines")
        assertThat(prompt).contains("Context YAML format")
        assertThat(prompt).contains("files:")
        assertThat(prompt).contains("path:")
        assertThat(prompt).contains("readOnly:")
    }

    @Test
    fun `getExistingPlanPrompt contains required instructions`() {
        // Given
        val relativePlanPath = "test/plan.md"
        
        // When
        val prompt = AiderPlanPromptTemplates.getExistingPlanPrompt(relativePlanPath)
        
        // Then
        assertThat(prompt).contains("A plan already exists")
        assertThat(prompt).contains("Continue implementing the existing plan test/plan.md")
        assertThat(prompt).contains("In case subplans are referenced")
        assertThat(prompt).contains("Start implementing before updating the checklist")
        assertThat(prompt).contains("Update the plan, checklist and context.yaml")
        assertThat(prompt).contains("Always keep the context.yaml up to date")
    }

    @Test
    fun `getNewPlanPrompt with subplans enabled contains required instructions`() {
        // Given
        val enableSubplans = true
        
        // When
        val prompt = AiderPlanPromptTemplates.getNewPlanPrompt(enableSubplans)
        
        // Then
        assertThat(prompt).contains("No plan exists yet")
        assertThat(prompt).contains("Write a detailed description")
        assertThat(prompt).contains("The main plan file should include these sections")
        assertThat(prompt).contains("Create subplans only if necessary")
        assertThat(prompt).contains("A feature requires many changes across plenty of components")
        assertThat(prompt).contains("Different team members could work on parts independently")
        assertThat(prompt).contains("A component needs its own detailed planning")
        assertThat(prompt).doesNotContain("Do not create subplans")
    }

    @Test
    fun `getNewPlanPrompt with subplans disabled contains required instructions`() {
        // Given
        val enableSubplans = false
        
        // When
        val prompt = AiderPlanPromptTemplates.getNewPlanPrompt(enableSubplans)
        
        // Then
        assertThat(prompt).contains("No plan exists yet")
        assertThat(prompt).contains("Write a detailed description")
        assertThat(prompt).contains("The main plan file should include these sections")
        assertThat(prompt).contains("Create the three required files for the plan")
        assertThat(prompt).contains("A main markdown file with the plan details")
        assertThat(prompt).contains("A checklist markdown file to track implementation progress")
        assertThat(prompt).contains("A context.yaml file listing all affected files")
        assertThat(prompt).doesNotContain("Do not create subplans")
        assertThat(prompt).doesNotContain("Create subplans only if necessary")
        assertThat(prompt).doesNotContain("A feature requires many changes across plenty of components")
    }

    @Test
    fun `constants match the values in AiderPlanPromptService and AiderPlanService`() {
        // When/Then
        assertThat(AiderPlanPromptTemplates.AIDER_PLAN_MARKER)
            .isEqualTo(AiderPlanPromptService.AIDER_PLAN_MARKER)
        
        assertThat(AiderPlanPromptTemplates.AIDER_PLAN_CHECKLIST_MARKER)
            .isEqualTo(AiderPlanPromptService.AIDER_PLAN_CHECKLIST_MARKER)
        
        assertThat(AiderPlanPromptTemplates.AIDER_PLANS_FOLDER)
            .isEqualTo(AiderPlanService.AIDER_PLANS_FOLDER)
    }
}
