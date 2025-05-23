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
    }
    val allFiles: List<FileData>
        get() = planFiles + contextFiles
    val id: String
        get() = mainPlanFile?.filePath ?: ""
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
        return openChecklistItems().isEmpty()
    }
    fun isPlanFamilyComplete(): Boolean {
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
        val ancestors = mutableListOf<AiderPlan>()
        var current = parentPlan
        while (current != null) {
            ancestors.add(current)
            current = current.parentPlan
        }
        return ancestors
    }

    fun isRootPlan(): Boolean {
        return parentPlan == null && 
            !getAllChildPlans().any { it.childPlans.any { child -> child.mainPlanFile?.filePath == mainPlanFile?.filePath } }
    }

    fun isDescendantOf(otherPlan: AiderPlan): Boolean {
        return generateSequence(this) { it.parentPlan }.any { it == otherPlan }
    }

    fun getNextUncompletedPlansInSameFamily(): List<AiderPlan> {
        if (!isPlanComplete()) return listOf(this)
        val uncompletedChildren = getAllChildPlans().filter { !it.isPlanComplete() }
        val uncompletedSiblings = findSiblingPlans().filter { !it.isPlanComplete() }
        val parentUncompletedPlans = parentPlan?.getNextUncompletedPlansInSameFamily() ?: emptyList()
        
        return uncompletedChildren + uncompletedSiblings + parentUncompletedPlans
    }

    fun createShortTooltip(): String {
        val checkedItems = totalChecklistItems() - openChecklistItems().size
        val status = if (isPlanComplete()) "Complete" else "In Progress"
        
        return buildString {
            append("<html><body style='width:300px'>")
            append("<b>${mainPlanFile?.filePath?.substringAfterLast('/')}</b><br>")
            append("Status: $status | Progress: $checkedItems/${totalChecklistItems()}<br>")
            
            if (parentPlan != null) {
                append("Parent: ${parentPlan.mainPlanFile?.filePath?.substringAfterLast('/')}<br>")
            }
            
            if (childPlans.isNotEmpty()) {
                val completedChildren = childPlans.count { it.isPlanComplete() }
                append("Children: $completedChildren/${childPlans.size} complete")
            }
            
            append("</body></html>")
        }
    }

    fun createTooltip(): String {
        val checkedItems = totalChecklistItems() - openChecklistItems().size
        val status = if (isPlanComplete()) "Complete" else "In Progress"
        val openItems = openChecklistItems()
        
        return buildString {
            append("<html><body style='width:400px'>")
            append("<b>${mainPlanFile?.filePath}</b><br>")
            append("Status: $status | Progress: $checkedItems/${totalChecklistItems()}<br>")
            
            // Add cost information if available
            mainPlanFile?.filePath?.let { planPath ->
                val costService = com.intellij.openapi.components.service<PlanExecutionCostService>()
                val executionHistory = costService.getExecutionHistory(planPath)
                val executionCount = executionHistory.size
                
                // Calculate totals directly from the execution history
                val totalCost = executionHistory.sumOf { it.sessionCost }
                val totalTokens = executionHistory.sumOf { it.tokensSent + it.tokensReceived }
                
                if (totalCost > 0 || totalTokens > 0) {
                    append("<br><b>Execution Stats:</b><br>")
                    append("Total Cost: $${String.format("%.4f", totalCost)} | ")
                    append("Total Tokens: ${if (totalTokens >= 1000) String.format("%,dk", totalTokens / 1000) else totalTokens} | ")
                    append("Executions: $executionCount<br>")
                }
            }
            
            append("<br>")
            
            if (openItems.isNotEmpty()) {
                append("<b>Open Items:</b><br>")
                openItems.take(3).forEach { item ->
                    append("• ${item.description.replace("<", "&lt;").replace(">", "&gt;")}<br>")
                }
                if (openItems.size > 3) {
                    append("<i>... and ${openItems.size - 3} more items</i><br>")
                }
            }
            
            if (isPlanComplete()) {
                append("<br><span style='color:green'>✓ All tasks completed!</span>")
            }
            
            append("</body></html>")
        }
    }
}

