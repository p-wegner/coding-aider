package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import java.io.File

data class ChecklistItem(val description: String, val checked: Boolean, val children: List<ChecklistItem>)
data class AiderPlan(val plan: String, val checklist: List<ChecklistItem>, val files: List<FileData>) {
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

    fun isPlanComplete() = openChecklistItems().isEmpty()
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
            return emptyList()
        }

        return plansDir.listFiles { file -> file.extension == "md" && !file.nameWithoutExtension.endsWith("_checklist") }
            ?.mapNotNull { file ->
                try {
                    val content = file.readText()
                    if (content.contains(AIDER_PLAN_MARKER)) {
                        // Extract plan content after the marker
                        val planContent = content.substringAfter(AIDER_PLAN_MARKER).trim()
                        
                        // Look for associated checklist file
                        val checklistFile = File(plansDir, "${file.nameWithoutExtension}_checklist.md")
                        val checklist = if (checklistFile.exists()) {
                            parseChecklist(checklistFile)
                        } else emptyList()
                        
                        // Include both plan and checklist files in the files list
                        val files = mutableListOf(FileData(file.absolutePath, false))
                        if (checklistFile.exists()) {
                            files.add(FileData(checklistFile.absolutePath, false))
                        }
                        
                        AiderPlan(
                            plan = planContent,
                            checklist = checklist,
                            files = files
                        )
                    } else null
                } catch (e: Exception) {
                    // Log error but continue processing other files
                    println("Error processing plan file ${file.name}: ${e.message}")
                    null
                }
            } ?: emptyList()
    }

    private fun parseChecklist(file: File): List<ChecklistItem> {
        val content = file.readText()
        if (!content.contains(AIDER_PLAN_CHECKLIST_MARKER)) return emptyList()
        
        // Extract content after the marker
        val checklistContent = content.substringAfter(AIDER_PLAN_CHECKLIST_MARKER).trim()
        val lines = checklistContent.lines()
        
        return parseChecklistItems(lines, 0, 0).first
    }

    private fun parseChecklistItems(
        lines: List<String>, 
        startIndex: Int, 
        parentIndent: Int
    ): Pair<List<ChecklistItem>, Int> {
        val items = mutableListOf<ChecklistItem>()
        var currentIndex = startIndex
        
        while (currentIndex < lines.size) {
            val line = lines[currentIndex]
            if (line.isBlank()) {
                currentIndex++
            } else {
            
            val indent = line.indentationLevel()
                // Return if we've moved back to a higher level
                if (indent < parentIndent) {
                    return Pair(items, currentIndex)
                }
                
                // Process checklist items at current level
                if (isChecklistItem(line)) {
                    val (item, nextIndex) = parseChecklistItem(lines, currentIndex)
                    items.add(item)
                    currentIndex = nextIndex
                } else {
                    currentIndex++
                }
            }
        }
        
        return Pair(items, lines.size)
    }
    
    private fun String.indentationLevel(): Int = 
        indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: length
    
    private fun isChecklistItem(line: String): Boolean = 
        line.trim().matches(Regex("""- \[([ xX])\].*""")) || 
        line.trim().matches(Regex("""\[([ xX])\].*"""))  // Also match without leading dash
    
    private fun parseChecklistItem(
        lines: List<String>, 
        currentIndex: Int
    ): Pair<ChecklistItem, Int> {
        val line = lines[currentIndex]
        val indent = line.indentationLevel()
        
        // Parse current item
        val checked = line.contains(Regex("""\[[xX]\]"""))
        val description = line.trim()
            .removePrefix("- ") // Remove dash if present
            .removePrefix("[")
            .substringAfter("]")
            .trim()
        
        // Look for nested items
        var nextIndex = currentIndex + 1
        val children = if (hasNestedItems(lines, nextIndex, indent)) {
            val (nestedItems, newIndex) = parseChecklistItems(lines, nextIndex, indent)
            nextIndex = newIndex
            nestedItems
        } else emptyList()
        
        return Pair(ChecklistItem(description, checked, children), nextIndex)
    }
    
    private fun hasNestedItems(
        lines: List<String>, 
        currentIndex: Int, 
        parentIndent: Int
    ): Boolean {
        val nextLine = lines.getOrNull(currentIndex)?.takeIf { it.isNotBlank() } ?: return false
        return nextLine.indentationLevel() > parentIndent && isChecklistItem(nextLine)
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
