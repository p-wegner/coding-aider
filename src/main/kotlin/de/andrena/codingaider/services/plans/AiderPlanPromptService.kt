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
        SYSTEM 2. feature_name_checklist.md - Progress tracking with - [ ] checkboxes
        SYSTEM 3. feature_name_context.yaml - Lists required implementation files
    """.trimIndent()

    private val subplanGuidancePrompt = """
        SYSTEM Subplan Requirements:
        SYSTEM 1. Name format: mainplan_subfeature (e.g. authentication_login)
        SYSTEM 2. Create all three files per subplan:
           - mainplan_subfeature.md
           - mainplan_subfeature_checklist.md 
           - mainplan_subfeature_context.yaml
        SYSTEM 3. Reference format in main plan:
           <!-- SUBPLAN:mainplan_subfeature -->
           [Subplan: Subfeature Name](mainplan_subfeature.md)
           <!-- END_SUBPLAN -->
        SYSTEM 4. Add to main checklist: - [ ] Complete subfeature implementation
        SYSTEM 5. Subplan checklists need:
           - Atomic tasks with - [ ] checkboxes
           - Implementation-specific details
           - Clear dependency markers
        SYSTEM 6. Ensure the main plan checklist properly delegates actual implementation to the subplans
        SYSTEM 7. Ensure the main plan references all subplans
        SYSTEM 8. Only create subplans if necessary
        
    """.trimIndent()

    fun createPlanRefinementPrompt(plan: AiderPlan, refinementRequest: String): String {
        val basePrompt = """
            SYSTEM You are refining an existing plan ${plan.plan}. The plan should be extended or modified based on the refinement request.
            SYSTEM The refinement request is: $refinementRequest
            SYSTEM Decide whether to use subplans.
            $subplanGuidancePrompt
            $planFileStructurePrompt
        """.trimIndent()

        return basePrompt
    }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val files = commandData.files

        val existingPlan = filterPlanRelevantFiles(files)

        val basePrompt = """
            SYSTEM You are working in a plan based mode with plan files in $AIDER_PLANS_FOLDER:
            
            SYSTEM File Requirements:
            1. Start plans with $AIDER_PLAN_MARKER
            2. Start checklists with $AIDER_PLAN_CHECKLIST_MARKER
            3. Checklist items should be atomic and use markdown checkboxes (i.e. - [ ] Taskdescription)
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
            
        """.trimStartingWhiteSpaces()

        val firstPlan = existingPlan.firstOrNull()
        val relativePlanPath = firstPlan?.filePath?.removePrefix(commandData.projectPath) ?: ""
        val planSpecificPrompt = firstPlan?.let {
            """
            SYSTEM A plan already exists. Continue implementing the existing plan $relativePlanPath and its checklist step by step.
            SYSTEM In case subplans are referenced, continue by implementing the subplans that match the current progress of the main plan. 
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
            
            SYSTEM Create subplans only if necessary. Use subplans when:
            1. A feature requires many changes across plenty of components
            2. Different team members could work on parts independently
            3. A component needs its own detailed planning
            $subplanGuidancePrompt
            SYSTEM Create separate checklist and context.yaml files for the main plan and each subplan to track the progress of implementing the plan.            
            SYSTEM For the context.yaml, consider all provided files and add relevant files to the affected context.yaml.
            SYSTEM Only proceed with changes after creating and committing the plan files.
            SYSTEM Ensure that you stick to the defined editing format when creating or editing files, e.g. only have the filepath above search blocks.
            SYSTEM Make sure to commit the creation of all plan files even if you think you need additional files to implement the plan.
            SYSTEM Don't start the implementation until the plan files are committed.
            """

        return """
${basePrompt.trimStartingWhiteSpaces()}
${planSpecificPrompt.trimStartingWhiteSpaces()}
$STRUCTURED_MODE_MESSAGE_MARKER ${commandData.message} $STRUCTURED_MODE_MESSAGE_END_MARKER
               """.trimStartingWhiteSpaces()
    }

    fun filterPlanRelevantFiles(files: List<FileData>): List<FileData> =
        files.filter {
            it.filePath.contains(AIDER_PLANS_FOLDER) && !it.filePath.contains(AiderPlanService.FINISHED_AIDER_PLANS_FOLDER)
                    && (it.filePath.endsWith(".md") || it.filePath.endsWith(".yaml"))
        }

    fun filterPlanMainFiles(files: List<FileData>): List<FileData> =
        filterPlanRelevantFiles(files).filter { it.filePath.endsWith(".md") && !it.filePath.endsWith("_checklist.md") }

    private fun String.trimStartingWhiteSpaces() = trimIndent().trimStart { it.isWhitespace() }
    // TODO: decide where to put this (see AiderPlanService)
    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLAN_CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        const val AIDER_PLANS_FOLDER = AiderPlanService.AIDER_PLANS_FOLDER
        const val STRUCTURED_MODE_MESSAGE_MARKER = "<STRUCTURED MODE>"
        const val STRUCTURED_MODE_MESSAGE_END_MARKER = "</STRUCTURED MODE>"
    }
}
