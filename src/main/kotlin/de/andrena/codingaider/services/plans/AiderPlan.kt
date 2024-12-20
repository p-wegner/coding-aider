package de.andrena.codingaider.services.plans

import de.andrena.codingaider.command.FileData

data class ChecklistItem(val description: String, val checked: Boolean, val children: List<ChecklistItem>)
data class AiderPlan(
    val plan: String,
    val checklist: List<ChecklistItem>,
    val planFiles: List<FileData>,
    val contextFiles: List<FileData>,
    val parentPlan: AiderPlan? = null,
    val childPlans: List<AiderPlan> = emptyList()
) {
    val allFiles: List<FileData>
        get() = planFiles + contextFiles

    val mainPlanFile: FileData?
        get() = planFiles.firstOrNull { it.filePath.endsWith(".md") && !it.filePath.endsWith("_checklist.md") }
    val checklistPlanFile: FileData?
        get() = planFiles.firstOrNull { it.filePath.endsWith("_checklist.md") }
    val contextYamlFile: FileData?
        get() = planFiles.firstOrNull { it.filePath.endsWith("_context.yaml") }


    fun openChecklistItems(): List<ChecklistItem> {
        return checklist.flatMap { item -> getAllOpenItems(item) }
    }

    private fun getAllOpenItems(item: ChecklistItem): List<ChecklistItem> {
        val result = mutableListOf<ChecklistItem>()
        if (!item.checked) result.add(item)
        item.children.forEach { child ->
            result.addAll(getAllOpenItems(child))
        }
        return result
    }

    fun totalChecklistItems(): Int {
        fun countItems(items: List<ChecklistItem>): Int {
            return items.sumOf { item -> 1 + countItems(item.children) }
        }
        return countItems(checklist)
    }

    fun isPlanComplete(): Boolean {
        return openChecklistItems().isEmpty() && childPlans.all { it.isPlanComplete() }
    }

    fun getAllChildPlans(): List<AiderPlan> {
        return childPlans + childPlans.flatMap { it.getAllChildPlans() }
    }

    fun findRootPlan(): AiderPlan {
        return parentPlan?.findRootPlan() ?: this
    }

    fun createShortTooltip(): String = buildString {
        appendLine("<html><body>")
        appendLine("<b>Plan:</b> ${mainPlanFile?.filePath}<br>")
        parentPlan?.mainPlanFile?.let { parent ->
            appendLine("<b>Parent Plan:</b> ${parent.filePath}<br>")
        }
        if (childPlans.isNotEmpty()) {
            appendLine("<b>Child Plans:</b> ${childPlans.size}<br>")
        }
        appendLine("<b>Status:</b> ${if (isPlanComplete()) "Complete" else "In Progress"}<br>")
        val checkedItems = totalChecklistItems() - openChecklistItems().size
        appendLine("<b>Progress:</b> $checkedItems/${totalChecklistItems()} items completed")
        appendLine("</body></html>")
    }

    fun createTooltip(): String = buildString {
        appendLine("<html><body style='width: 400px'>")
        appendLine("<b>Plan:</b> ${mainPlanFile?.filePath}<br>")
        appendLine("<b>Status:</b> ${if (isPlanComplete()) "Complete" else "In Progress"}<br>")
        val checkedItems = totalChecklistItems() - openChecklistItems().size
        appendLine("<b>Progress:</b> $checkedItems/${totalChecklistItems()} items completed<br>")
        appendLine("<br><b>Open Items:</b><br>")
        openChecklistItems().take(5).forEach { item ->
            appendLine("• ${item.description.replace("<", "&lt;").replace(">", "&gt;")}<br>")
        }
        if (openChecklistItems().size > 5) {
            appendLine("<i>... and ${openChecklistItems().size - 5} more items</i><br>")
        }
        appendLine("<br><b>Description:</b><br>")
        val planPreview = plan.lines().take(3).joinToString("\n").let {
            if (it.length > 200) it.take(200) + "..." else it
        }
        appendLine(planPreview.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>"))

        if (isPlanComplete()) {
            appendLine("<br><br><span style='color: green'>✓ All tasks completed!</span>")
        }
        appendLine("</body></html>")
    }
}

