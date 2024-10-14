package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData

@Service(Service.Level.PROJECT)
class AiderPlanService(private val project: Project) {
    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLANS_FOLDER = ".coding-aider-plans"
        const val STRUCTURED_MODE_MARKER = "[STRUCTURED MODE]"

        @JvmStatic
        fun getInstance(project: Project): AiderPlanService = project.service()
    }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val existingPlan =
            commandData.files.firstOrNull { it.filePath.contains(AIDER_PLANS_FOLDER) && it.filePath.endsWith(".md") }

        val basePrompt = """
            SYSTEM Instead of making changes to the code, markdown files should be used to track progress on the feature.
            SYSTEM A plan consists of a detailed description of the requested feature and a separate file with a checklist for tracking the progress.
            SYSTEM The file should be saved in the $AIDER_PLANS_FOLDER directory in the project.
            SYSTEM Always start plans with the line $AIDER_PLAN_MARKER at the beginning of the file and use this marker in existing files to identify plans.
            SYSTEM Never proceed with changes if the plan is not committed yet.
            SYSTEM Once the plan properly describes the changes, start implementing them step by step. Commit each change as you go.
        """.trimIndent()

        val planSpecificPrompt = existingPlan?.let {
            """
            SYSTEM A plan already exists. Continue implementing the existing plan ${existingPlan.filePath} step by step.
            SYSTEM Start implementing before updating the checklist. If no changes are done don't update the checklist.
            SYSTEM In that case inform the user why no changes were made.
            SYSTEM New files that are not the plan and are not part of the checklist should be created in a suitable location.
            SYSTEM If no further information is given, use ${commandData.projectPath} as the location.
            SYSTEM Update the plan and checklist as needed based on the current progress and any new requirements.
            SYSTEM If the current instruction doesn't align with the existing plan, update the plan accordingly before proceeding.
            """

        } ?: """
            SYSTEM No plan exists yet. Write a detailed description of the requested feature and the needed changes.
            SYSTEM Save the plan in a new markdown file with a suitable name in the $AIDER_PLANS_FOLDER directory.
            SYSTEM Create a separate checklist file to track the progress of implementing the plan.
            SYSTEM Only proceed with changes after creating and committing the plan.
            """

        return """
            $basePrompt
            $planSpecificPrompt
            $STRUCTURED_MODE_MARKER ${commandData.message}
        """.trimIndent()
    }
}
