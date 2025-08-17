package de.andrena.codingaider.services.plans

/**
 * Contains all prompt templates used by the AiderPlanPromptService.
 * Externalizing these templates makes them easier to maintain and update.
 */
class AiderPlanPromptTemplates(private val planService: AiderPlanService) {

    val planFileStructurePrompt = """
Each plan requires three files with consistent naming (feature_name as base):
1. feature_name.md - Main plan with sections:
   - Overview
   - Problem Description
   - Goals
   - Additional Notes
   - References
2. feature_name_checklist.md - Progress tracking with - [ ] checkboxes
3. feature_name_context.yaml - Lists required implementation files
    """.trimIndent()

    val subplanGuidancePrompt = """
Subplan Requirements:
1. Name format: mainplan_subfeature (e.g. authentication_login)
2. Create all three files per subplan:
   - mainplan_subfeature.md
   - mainplan_subfeature_checklist.md
   - mainplan_subfeature_context.yaml
3. Reference format in main plan:
   <!-- SUBPLAN:mainplan_subfeature -->
   [Subplan: Subfeature Name](mainplan_subfeature.md)
   <!-- END_SUBPLAN -->
4. Add to main checklist: - [ ] Complete subfeature implementation
5. Subplan checklists need:
   - Atomic tasks with - [ ] checkboxes
   - Implementation-specific details
   - Clear dependency markers
6. Ensure the main plan checklist properly delegates actual implementation to the subplans
7. Ensure the main plan references all subplans
8. Only create subplans if necessary
    """.trimIndent()

    val noSubplansGuidancePrompt =
        """Create a detailed checklist with atomic tasks that focus on clear, sequential implementation steps.
""".trimIndent()

    val planFileFormatPrompt = """
Plan files are located in ${planService.getAiderPlansFolder()}:

File Requirements:
1. Start plans with $AIDER_PLAN_MARKER
2. Start checklists with $AIDER_PLAN_CHECKLIST_MARKER
3. Checklist items should be atomic and use markdown checkboxes (i.e. - [ ] Taskdescription)
3. Use consistent naming: feature_name.md, _checklist.md, _context.yaml
4. Cross-reference files using markdown links

## Content Guidelines:
- Plans: High-level feature descriptions and major implementation details
- Checklists: Fine-grained implementation steps
- Context YAML format:
```
  ---
  files:
  - path: "full/path/to/file"
    readOnly: false
```
"""

    fun getExistingPlanPrompt(relativePlanPath: String) = """
A plan already exists. Continue implementing the existing plan $relativePlanPath and its checklist step by step.  
In case subplans are referenced, continue by implementing the subplans that match the current progress of the main plan.   
Start implementing before updating the checklist. If no changes are done, don't update the checklist.  
In that case inform the user why no changes were made.  
New files that are not the plan and are not part of the checklist should be created in a suitable location and added to the context.yaml.  
If no further information is given, use the project path as the location.  
Update the plan, checklist and context.yaml as needed based on the current progress and any new requirements.  
Important: Always keep the context.yaml up to date with your changes. If files are created or edited, add them to the context.yaml.  
If the current instruction doesn't align with the existing plan, update the plan accordingly before proceeding.  
    """.trimIndent()

    fun getNewPlanPrompt(enableSubplans: Boolean) = """
No plan exists yet. Write a detailed description of the requested feature and the needed changes.
The main plan file should include these sections: ## Overview, ## Problem Description, ## Goals, ## Additional Notes and Constraints, ## References
Save the plan in a new markdown file with a suitable name in the ${planService.getAiderPlansFolder()} directory.

${
        if (enableSubplans) {
            """
Create subplans only if necessary. Use subplans when:
1. A feature requires many changes across plenty of components
2. Different team members could work on parts independently
3. A component needs its own detailed planning
$subplanGuidancePrompt
Create separate checklist and context.yaml files for the main plan and each subplan to track the progress of implementing the plan.
"""
        } else {
            ""
        }
    }
Create the three required files for the plan:
1. A main markdown file with the plan details
2. A checklist markdown file to track implementation progress
3. A context.yaml file listing all affected files

For the context.yaml, consider all provided files and add relevant files to the affected context.yaml.
Only proceed with changes after creating and committing the plan files.
Ensure that you stick to the defined editing format when creating or editing files, e.g. only have the filepath above search blocks.
Make sure to commit the creation of all plan files even if you think you need additional files to implement the plan.
Don't start the implementation until the plan files are committed. Do not ask the user if he wants to proceed with the plan. Create the plan files and stop!
    """.trimIndent()

    // Constants used in the templates
}

const val AIDER_PLAN_MARKER = AiderPlanPromptService.AIDER_PLAN_MARKER
const val AIDER_PLAN_CHECKLIST_MARKER = AiderPlanPromptService.AIDER_PLAN_CHECKLIST_MARKER
