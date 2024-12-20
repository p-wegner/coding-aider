package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData

@Service(Service.Level.PROJECT)
class AiderPlanPromptService(private val project: Project) {

    fun createPlanRefinementPrompt(plan: AiderPlan, refinementRequest: String): String {
        val basePrompt = """
            SYSTEM You are refining an existing plan. The plan should be extended or modified based on the refinement request.
            SYSTEM If the refinement requires significant new functionality:
            SYSTEM 1. Create a subplan with _subplan suffix (e.g. feature_subplan1.md)
            SYSTEM 2. Reference the subplan from the main plan using markdown links
            SYSTEM 3. Create separate checklist and context.yaml files for the subplan
            SYSTEM 4. Update the main plan's checklist to track the subplan's completion
            SYSTEM The refinement request is: $refinementRequest
            
            $STRUCTURED_MODE_MARKER Continue with plan refinement
        """.trimIndent()
        
        return basePrompt
    }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val files = commandData.files

        val existingPlan = filterPlanRelevantFiles(files)

        val basePrompt = """
            SYSTEM Instead of making changes to the code, markdown files should be used to track progress on the feature.
            SYSTEM A plan consists of three files:
            SYSTEM 1. A detailed description of the requested feature
            SYSTEM 2. A separate file with a checklist for tracking the progress
            SYSTEM 3. A context.yaml file listing all relevant files needed for implementing the plan
            SYSTEM The file should be saved in the $AIDER_PLANS_FOLDER directory in the project.
            SYSTEM Always start plans with the line $AIDER_PLAN_MARKER and checklists with $AIDER_PLAN_CHECKLIST_MARKER at the beginning of the file and use this marker in existing files to identify plans and checklists.
            SYSTEM The plan should focus on high level descriptions of the requested features and major implementation details.
            SYSTEM The checklist should focus on the required implementation steps on a more fine grained level.
            SYSTEM The three files should be named consistently:
            SYSTEM - feature_name.md (plan)
            SYSTEM - feature_name_checklist.md (checklist)
            SYSTEM - feature_name_context.yaml (file list)
            SYSTEM The plan and checklist should reference each other using markdown file references.
            SYSTEM The context.yaml should list all files that will be needed to implement the plan.
            SYSTEM The context.yaml must follow this format:
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
            SYSTEM If refining an existing plan, create a subplan with _subplan suffix (e.g. feature_subplan1.md).
            SYSTEM The subplan should follow the same structure as the main plan but focus on specific components.
            SYSTEM Reference the subplan from the main plan using markdown links.
            SYSTEM If the feature is complex, break it down into subplans:
            SYSTEM - Create a main plan describing the overall feature
            SYSTEM - Create subplans for major components using _subplan suffix (e.g. feature_subplan1.md)
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
