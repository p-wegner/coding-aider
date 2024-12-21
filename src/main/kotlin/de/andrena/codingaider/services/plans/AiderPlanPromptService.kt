package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData

@Service(Service.Level.PROJECT)
class AiderPlanPromptService(private val project: Project) {

    private val planFileStructurePrompt = """
        SYSTEM A plan consists of three files:
        SYSTEM 1. A detailed description of the requested feature (.md)
        SYSTEM 2. A separate file with a checklist for tracking the progress (_checklist.md)
        SYSTEM 3. A context.yaml file listing all relevant files needed for implementing the plan (_context.yaml)
        SYSTEM The files should be named consistently with the same base name:
        SYSTEM - feature_name.md (plan) - Contains sections: Overview, Problem Description, Goals, Additional Notes, References
        SYSTEM - feature_name_checklist.md (checklist) - Contains clear, actionable tasks with [ ] checkboxes
        SYSTEM - feature_name_context.yaml (file list) - Lists all files needed for implementation
        SYSTEM For subplans, use: mainplan_subfeature naming pattern (e.g. authentication_login.md)
    """.trimIndent()

    private val subplanGuidancePrompt = """
        SYSTEM When creating subplans:
        SYSTEM 1. Use mainplan_subfeature naming pattern (e.g. authentication_login.md)
        SYSTEM 2. ALWAYS create all three files for each subplan:
        SYSTEM   - mainplan_subfeature.md (subplan description with standard sections)
        SYSTEM   - mainplan_subfeature_checklist.md (subplan specific checklist)
        SYSTEM   - mainplan_subfeature_context.yaml (subplan relevant files)
        SYSTEM 3. Reference subplans from main plan using markdown links: [Subplan Name](mainplan_subfeature.md)
        SYSTEM 4. Update main plan's checklist to track subplan completion with a task: [ ] Complete subfeature implementation
        SYSTEM 5. Each subplan's checklist must have:
        SYSTEM   - Clear, atomic tasks with [ ] checkboxes
        SYSTEM   - Tasks focused on specific implementation details
        SYSTEM   - Dependencies noted at the start of dependent tasks
        SYSTEM 6. Main plan should track overall progress while subplans handle specific components
    """.trimIndent()

    fun createPlanRefinementPrompt(plan: AiderPlan, refinementRequest: String): String {
        val basePrompt = """
            SYSTEM You are refining an existing plan. The plan should be extended or modified based on the refinement request.
            
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

        val contextYamlFormatPrompt = """
            SYSTEM The context.yaml must follow this format and be kept up to date:
            SYSTEM ---
            SYSTEM files:
            SYSTEM - path: "full/path/to/file"
            SYSTEM   readOnly: false
            SYSTEM - path: "full/path/to/another/file"
            SYSTEM   readOnly: true
            SYSTEM Be sure to use correct relative path (same folder) references between the files.
            SYSTEM Once the plan properly describes the changes, start implementing them step by step.
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
            SYSTEM Create separate checklist and context.yaml files to track the progress of implementing the plan.
            SYSTEM If refining an existing plan, create a subplan with main plan name as prefix (e.g. mainplan_feature1.md).
            SYSTEM The subplan should follow the same structure as the main plan but focus on specific components.
            SYSTEM Reference the subplan from the main plan using markdown links.
            SYSTEM If the feature is complex, break it down into subplans:
            SYSTEM - Create a main plan describing the overall feature
            SYSTEM - Create subplans for major components 
            SYSTEM - Reference subplans from the main plan using markdown links
            SYSTEM - Each subplan should have its own checklist and context.yaml files
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
