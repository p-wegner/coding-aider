package de.andrena.codingaider.services.plans

import de.andrena.codingaider.command.FileData

data class ChecklistItem(val description: String, val checked: Boolean, val children: List<ChecklistItem>)
data class AiderPlan(
    val plan: String,
    val checklist: List<ChecklistItem>,
    val planFiles: List<FileData>,
    val contextFiles: List<FileData>,
    val parentPlan: AiderPlan? = null,
    val childPlans: List<AiderPlan> = emptyList(),
    val depth: Int = calculateDepth(parentPlan)
) {
    companion object {
        private fun calculateDepth(parent: AiderPlan?): Int = parent?.depth?.plus(1) ?: 0
        
        fun findCommonAncestor(plan1: AiderPlan, plan2: AiderPlan): AiderPlan? {
            val ancestors1 = generateSequence(plan1) { it.parentPlan }.toSet()
            return generateSequence(plan2) { it.parentPlan }.find { it in ancestors1 }
        }
    }
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

    fun findSiblingPlans(): List<AiderPlan> {
        return parentPlan?.childPlans?.filter { it != this } ?: emptyList()
    }

    fun getAncestors(): List<AiderPlan> {
        return generateSequence(parentPlan) { it.parentPlan }.toList()
    }

    fun isDescendantOf(otherPlan: AiderPlan): Boolean {
        return generateSequence(this) { it.parentPlan }.any { it == otherPlan }
    }

    fun getNextUncompletedPlan(): AiderPlan? {
        if (!isPlanComplete()) return this
        return getAllChildPlans().firstOrNull { !it.isPlanComplete() }
            ?: findSiblingPlans().firstOrNull { !it.isPlanComplete() }
            ?: parentPlan?.getNextUncompletedPlan()
    }

    fun createShortTooltip(): String = buildString {
        appendLine("<html><body>")
        appendLine("<b>Plan:</b> ${mainPlanFile?.filePath}<br>")
        parentPlan?.mainPlanFile?.let { parent ->
            appendLine("<b>Parent Plan:</b> ${parent.filePath}<br>")
        }
        if (childPlans.isNotEmpty()) {
            appendLine("<b>Child Plans:</b> ${childPlans.size}<br>")
            appendLine("<b>Child Plans Status:</b> ${childPlans.count { it.isPlanComplete() }}/${childPlans.size} completed<br>")
        }
        appendLine("<b>Status:</b> ${if (isPlanComplete()) "Complete" else "In Progress"}<br>")
        val checkedItems = totalChecklistItems() - openChecklistItems().size
        appendLine("<b>Progress:</b> $checkedItems/${totalChecklistItems()} items completed")
        appendLine("<b>Depth:</b> Level $depth")
        appendLine("</body></html>")
    }

    fun createTooltip(): String = buildString {
        appendLine("<html><body style='width: 400px'>")
        appendLine("<b>Plan:</b> ${mainPlanFile?.filePath}<br>")
        appendLine("<b>Status:</b> ${if (isPlanComplete()) "Complete" else "In Progress"}<br>")
        val checkedItems = totalChecklistItems() - openChecklistItems().size
        appendLine("<b>Progress:</b> $checkedItems/${totalChecklistItems()} items completed<br>")
        if (parentPlan != null) {
            appendLine("<b>Parent Plan:</b> ${parentPlan.mainPlanFile?.filePath}<br>")
        }
        if (childPlans.isNotEmpty()) {
            appendLine("<b>Child Plans:</b><br>")
            childPlans.forEach { child ->
                appendLine("• ${child.mainPlanFile?.filePath} (${if (child.isPlanComplete()) "Complete" else "In Progress"})<br>")
            }
        }
        if (parentPlan != null) {
            appendLine("<b>Parent Plan:</b> ${parentPlan.mainPlanFile?.filePath}<br>")
        }
        if (childPlans.isNotEmpty()) {
            appendLine("<b>Child Plans:</b><br>")
            childPlans.forEach { child ->
                appendLine("• ${child.mainPlanFile?.filePath} (${if (child.isPlanComplete()) "Complete" else "In Progress"})<br>")
            }
        }
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

