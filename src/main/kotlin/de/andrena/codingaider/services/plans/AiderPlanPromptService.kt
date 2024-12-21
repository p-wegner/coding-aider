package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData

@Service(Service.Level.PROJECT)
class AiderPlanPromptService(private val project: Project) {

    private val planFileStructurePrompt = """
        SYSTEM Each plan requires three files with consistent naming (feature_name as base):
        SYSTEM 1. feature_name.md - Main plan with sections:
           - Overview
           - Problem Description
           - Goals
           - Additional Notes
           - References
        SYSTEM 2. feature_name_checklist.md - Progress tracking with [ ] checkboxes
        SYSTEM 3. feature_name_context.yaml - Lists required implementation files
    """.trimIndent()

    private val subplanGuidancePrompt = """
        SYSTEM Subplan Requirements:
        SYSTEM 1. Name format: mainplan_subfeature (e.g. authentication_login)
        SYSTEM 2. Create all three files per subplan:
           - mainplan_subfeature.md
           - mainplan_subfeature_checklist.md 
           - mainplan_subfeature_context.yaml
        SYSTEM 3. Link from main plan: [Subplan Name](mainplan_subfeature.md)
        SYSTEM 4. Add to main checklist: [ ] Complete subfeature implementation
        SYSTEM 5. Subplan checklists need:
           - Atomic tasks with [ ] checkboxes
           - Implementation-specific details
           - Clear dependency markers
    """.trimIndent()

    fun createPlanRefinementPrompt(plan: AiderPlan, refinementRequest: String): String {
        val basePrompt = """
            SYSTEM You are refining an existing plan ${plan.plan}. The plan should be extended or modified based on the refinement request.
            
            $subplanGuidancePrompt
            
            SYSTEM The refinement request is: $refinementRequest
            
            $planFileStructurePrompt
            
            $STRUCTURED_MODE_MARKER Continue with plan refinement but don't start implementing the changes. Focus on changes to plan files.
        """.trimIndent()

        return basePrompt
    }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val files = commandData.files

        val existingPlan = filterPlanRelevantFiles(files)

        val basePrompt = """
            SYSTEM Create plan files in $AIDER_PLANS_FOLDER before making code changes:
            
            SYSTEM File Requirements:
            1. Start plans with $AIDER_PLAN_MARKER
            2. Start checklists with $AIDER_PLAN_CHECKLIST_MARKER
            3. Use consistent naming: feature_name.md, _checklist.md, _context.yaml
            4. Cross-reference files using markdown links
            
            SYSTEM Content Guidelines:
            - Plans: High-level feature descriptions and major implementation details
            - Checklists: Fine-grained implementation steps
            - Context YAML format:
              ---
              files:
              - path: "full/path/to/file"
                readOnly: false
            
            SYSTEM Implementation:
            1. Create/update plan files first
            2. Implement changes step by step
            3. Keep context.yaml current with all needed files
        """.trimStartingWhiteSpaces()

        val firstPlan = existingPlan.firstOrNull()
        val relativePlanPath = firstPlan?.filePath?.removePrefix(commandData.projectPath) ?: ""
        val planSpecificPrompt = firstPlan?.let {
            """
            SYSTEM A plan already exists. Continue implementing the existing plan $relativePlanPath and its checklist step by step.
            SYSTEM Start implementing before updating the checklist. If no changes are done, don't update the checklist.
            SYSTEM In that case inform the user why no changes were made.
            SYSTEM New files that are not the plan and are not part of the checklist should be created in a suitable location and added to the context.yaml.
            SYSTEM If no further information is given, use ${commandData.projectPath} as the location.
            SYSTEM Update the plan, checklist and context.yaml as needed based on the current progress and any new requirements.
            SYSTEM Important: Always keep the context.yaml up to date with your changes. If files are created or edited, add them to the context.yaml.
            SYSTEM If the current instruction doesn't align with the existing plan, update the plan accordingly before proceeding.
            """

        } ?: """
            SYSTEM No plan exists yet. Write a detailed description of the requested feature and the needed changes.
            SYSTEM The main plan file should include these sections: ## Overview, ## Problem Description, ## Goals, ## Additional Notes and Constraints, ## References 
            SYSTEM Save the plan in a new markdown file with a suitable name in the $AIDER_PLANS_FOLDER directory.
            
            SYSTEM Create subplans when:
            1. A feature requires changes across multiple distinct components
            2. Implementation involves separate logical phases
            3. Different team members could work on parts independently
            4. A component needs its own detailed planning
            5. Changes affect more than 3-4 files
            
            SYSTEM Create separate checklist and context.yaml files for the main plan and each subplan to track the progress of implementing the plan.            
            SYSTEM For the context.yaml, consider all provided files and add relevant files to the context.yaml.
            SYSTEM Only proceed with changes after creating and committing the plan files.
            SYSTEM Ensure that you stick to the defined editing format when creating or editing files, e.g. only have the filepath above search blocks.
            """

        return """
${basePrompt.trimStartingWhiteSpaces()}
${planSpecificPrompt.trimStartingWhiteSpaces()}
$STRUCTURED_MODE_MARKER ${commandData.message}
            """.trimStartingWhiteSpaces()
    }

    fun filterPlanRelevantFiles(files: List<FileData>): List<FileData> =
        files.filter {
            it.filePath.contains(AIDER_PLANS_FOLDER) && (it.filePath.endsWith(".md") || it.filePath.endsWith(
                ".yaml"
            ))
        }

    fun filterPlanMainFiles(files: List<FileData>): List<FileData> =
        filterPlanRelevantFiles(files).filter { it.filePath.endsWith(".md") && !it.filePath.endsWith("_checklist.md") }

    private fun String.trimStartingWhiteSpaces() = trimIndent().trimStart { it.isWhitespace() }

    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLAN_CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        const val AIDER_PLANS_FOLDER = ".coding-aider-plans"
        const val STRUCTURED_MODE_MARKER = "[STRUCTURED MODE]"
    }
}
