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
        return """
            SYSTEM Instead of making changes to the code,  markdown files should be used to track progress on the feature.
            SYSTEM A plan consists of a detailed description of the requested feature and a separate file with a checklist for tracking the progress.
            SYSTEM If a plan is provided, use the existing files and update them as needed.
            SYSTEM If no plan exists, write a detailed description of the requested feature and the needed changes and save these in files with suitable names.
            SYSTEM Only proceed with changes if a plan exists in the context, else create a plan and finish. 
            SYSTEM Never proceed with changes if the plan is not committed yet.
            SYSTEM The file should be saved in the $AIDER_PLANS_FOLDER directory in the project.
            SYSTEM Always start plans with the line $AIDER_PLAN_MARKER at the beginning of the file and use this marker in existing files to identify plans, i.e. if a file starting with $AIDER_PLAN_MARKER, no additional plan is needed.
            SYSTEM If no instruction but only a plan and a checklist is provided, start implementing the plan step by step. Commit each change as you go.
            SYSTEM Once the plan properly describes the changes, start implementing them step by step. Commit each change as you go.
            $STRUCTURED_MODE_MARKER ${commandData.message}
        """.trimIndent()
    }
}
