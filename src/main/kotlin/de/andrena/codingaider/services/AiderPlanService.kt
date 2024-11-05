package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import java.io.File

@Service(Service.Level.PROJECT)
class AiderPlanService(private val project: Project) {
    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLAN_CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        const val AIDER_PLANS_FOLDER = ".coding-aider-plans"
        const val STRUCTURED_MODE_MARKER = "[STRUCTURED MODE]"

    }

    fun createPlanFolderIfNeeded(commandData: CommandData) {
        if (commandData.structuredMode) {
            val plansDir = File(commandData.projectPath, AIDER_PLANS_FOLDER)
            if (!plansDir.exists()) {
                plansDir.mkdir()
            }
        }
    }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val existingPlan =
            commandData.files.firstOrNull { it.filePath.contains(AIDER_PLANS_FOLDER) && it.filePath.endsWith(".md") }

        val s = """
            SYSTEM Instead of making changes to the code, markdown files should be used to track progress on the feature.
            SYSTEM A plan consists of a detailed description of the requested feature and a separate file with a checklist for tracking the progress.
            SYSTEM The file should be saved in the $AIDER_PLANS_FOLDER directory in the project.
            SYSTEM Always start plans with the line $AIDER_PLAN_MARKER and checklists with $AIDER_PLAN_CHECKLIST_MARKER at the beginning of the file and use this marker in existing files to identify plans and checklists.
            SYSTEM The plan should focus on high level descriptions of the requested features and major implementation details.
            SYSTEM The checklist should focus on the required implementation steps on a more fine grained level.
            SYSTEM If a separate checklist exists, it is referenced in the plan using markdown file references.
            SYSTEM Likewise the plan is referenced in the checklist using markdown file references. Be sure to use correct relative path (same folder) references between the files, so assume the checklist is in the same folder as the plan.
            SYSTEM Never proceed with changes if the plan is not committed yet.
            SYSTEM Once the plan properly describes the changes, start implementing them step by step. Commit each change as you go.
        """
        val basePrompt = s.trimStartingWhiteSpaces()

        val planSpecificPrompt = existingPlan?.let {
            """
            SYSTEM A plan already exists. Continue implementing the existing plan ${existingPlan.filePath} and its checklist step by step.
            SYSTEM Start implementing before updating the checklist. If no changes are done, don't update the checklist.
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
${basePrompt.trimStartingWhiteSpaces()}
${planSpecificPrompt.trimStartingWhiteSpaces()}
$STRUCTURED_MODE_MARKER ${commandData.message}
            """.trimStartingWhiteSpaces()
    }

    private fun String.trimStartingWhiteSpaces() = trimIndent().trimStart { it.isWhitespace() }

}
