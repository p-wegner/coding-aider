package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import java.io.File

data class ChecklistItem(val description: String, val checked: Boolean, val children: List<ChecklistItem>)
data class AiderPlan(val plan: String, val checklist: List<ChecklistItem>, val files: List<FileData>){
    fun openChecklistItems(): List<ChecklistItem>{
        // check recursively if all children are checked
        return checklist.filter { !it.checked }
    }
    fun isPlanComplete() = checklist.all { it.checked }

}


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
    fun getAiderPlans(): List<AiderPlan> {
        val plansDir = File(project.basePath, AIDER_PLANS_FOLDER)
        if (!plansDir.exists()) {
            plansDir.mkdir()
        }
        // TODO: Read files in folder and parse them, each plan could have links to a checklist,
        // TODO: parse checklists and return them as AiderPlan objects

        return emptyList()
    }
    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val files = commandData.files

        val existingPlan = getExistingPlans(files)

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

        val firstPlan = existingPlan.firstOrNull()
        val relativePlanPath = firstPlan?.filePath?.removePrefix(commandData.projectPath) ?: ""
        val planSpecificPrompt = firstPlan?.let {
            """
            SYSTEM A plan already exists. Continue implementing the existing plan $relativePlanPath and its checklist step by step.
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

    fun getExistingPlans(files: List<FileData>) =
        files.filter { it.filePath.contains(AIDER_PLANS_FOLDER) && it.filePath.endsWith(".md") }

    private fun String.trimStartingWhiteSpaces() = trimIndent().trimStart { it.isWhitespace() }

}
[Coding Aider Plan]
# Display Available Plans in Toolwindow

This plan outlines the implementation of displaying all available Aider plans and their checklists in the Toolwindow below the persistent files section.

## Overview
- Add a new section in the Toolwindow to display plans and checklists
- Parse markdown files to identify plans and checklists
- Calculate and show plan completion status
- Allow navigation to plan and checklist files

## Technical Details
1. Extend PersistentFilesToolWindow to include a plans section
2. Use AiderPlanService to load and parse plans/checklists
3. Create a custom renderer for plans display
4. Add navigation capabilities to open plan/checklist files

## Implementation Approach
- Use existing AiderPlanService functionality
- Create a tree-like structure to show plans with their associated checklists
- Calculate completion status based on checklist items
- Provide visual indicators for plan status

See the detailed implementation steps in the [checklist](display_plans_in_toolwindow_checklist.md).
[Coding Aider Plan - Checklist]
# Display Plans in Toolwindow Implementation Checklist

Related to: [Plan](display_plans_in_toolwindow.md)

## Implementation Steps

- [ ] Extend PersistentFilesToolWindow
  - [ ] Add plans section below persistent files
  - [ ] Create plans list component
  - [ ] Add custom renderer for plans

- [ ] Enhance AiderPlanService
  - [ ] Implement plan file parsing
  - [ ] Add checklist parsing
  - [ ] Calculate plan completion status

- [ ] Create UI Components
  - [ ] Design plan item renderer
  - [ ] Add completion status indicator
  - [ ] Implement double-click navigation

- [ ] Add User Interactions
  - [ ] Enable plan file opening
  - [ ] Add checklist navigation
  - [ ] Implement refresh functionality

- [ ] Testing and Documentation
  - [ ] Add unit tests for new functionality
  - [ ] Update documentation
  - [ ] Test with various plan formats
