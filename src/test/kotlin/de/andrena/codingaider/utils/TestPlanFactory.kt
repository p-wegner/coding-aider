package de.andrena.codingaider.utils

import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.ChecklistItem
import java.io.File

object TestPlanFactory {

    fun createRootPlanWithSubplans(): AiderPlan {
        val authSubplan = AiderPlan(
            plan = "# Authentication System Implementation...",
            checklist = listOf(
                ChecklistItem("Implement JWT token generation", false, emptyList()),
                ChecklistItem("Implement JWT token validation", false, emptyList()),
            ),
            planFiles = listOf(FileData("src/test/resources/plans/multi_file_plans/auth_subplan.md", false)),
            contextFiles = emptyList()
        )

        val uiSubplan = AiderPlan(
            plan = "# UI Components Implementation...",
            checklist = listOf(
                ChecklistItem("Create Button component", false, emptyList()),
                ChecklistItem("Create Input component", false, emptyList()),
            ),
            planFiles = listOf(FileData("src/test/resources/plans/multi_file_plans/ui_subplan.md", false)),
            contextFiles = emptyList()
        )

        return AiderPlan(
            plan = "# Authentication and UI System Implementation...",
            checklist = listOf(
                ChecklistItem("Complete authentication system implementation", false, emptyList()),
                ChecklistItem("Complete UI components implementation", false, emptyList()),
            ),
            planFiles = listOf(FileData("src/test/resources/plans/multi_file_plans/root_plan_with_subplans.md", false)),
            contextFiles = emptyList(),
            childPlans = listOf(authSubplan, uiSubplan)
        )
    }
}
