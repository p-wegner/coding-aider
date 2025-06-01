package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.model.ContextFileHandler
import java.io.File
import java.time.LocalDateTime


@Service(Service.Level.PROJECT)
class AiderPlanService(private val project: Project) {
    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLAN_CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        const val AIDER_PLANS_FOLDER = ".coding-aider-plans"
        const val FINISHED_AIDER_PLANS_FOLDER = ".coding-aider-plans-finished"
        const val STRUCTURED_MODE_MARKER = "[STRUCTURED MODE]"
        const val SUBPLAN_START_MARKER = "<!-- SUBPLAN:"
        const val SUBPLAN_END_MARKER = "<!-- END_SUBPLAN -->"
    }

    fun createPlanFolderIfNeeded(commandData: CommandData) {
        if (commandData.structuredMode) {
            val plansDir = File(commandData.projectPath, AIDER_PLANS_FOLDER)
            if (!plansDir.exists()) {
                plansDir.mkdir()
            }
        }
    }
    
    /**
     * Tracks the creation of a new plan by recording an execution cost entry
     */
    fun trackPlanCreation(commandData: CommandData) {
        // Create a default execution cost entry for plan creation
        val costData = ExecutionCostData(
            timestamp = LocalDateTime.now(),
            tokensSent = 0,  // Will be updated when the actual response comes back
            tokensReceived = 0,  // Will be updated when the actual response comes back
            messageCost = 0.0,  // Will be updated when the actual response comes back
            sessionCost = 0.0,  // Will be updated when the actual response comes back
            model = commandData.llm,
            summary = "Plan creation initiated"
        )
        
        // We don't have a plan ID yet, so we'll create a temporary record
        // The actual plan cost will be updated when the command completes
        project.service<PlanExecutionCostService>().recordInitialPlanCreation(costData, commandData)
    }

    private fun extractChecklistItems(content: String): List<ChecklistItem> {
        val lines = content.lines()
        val (items, _) = parseChecklistItems(lines, 0, -1)
        return items
    }

    private fun parseChecklistItems(
        lines: List<String>,
        startIndex: Int,
        parentIndent: Int
    ): Pair<List<ChecklistItem>, Int> {
        val items = mutableListOf<ChecklistItem>()
        var currentIndex = startIndex

        while (currentIndex < lines.size) {
            val line = lines[currentIndex].trimEnd()
            if (line.isBlank()) {
                currentIndex++
                continue
            }

            val indent = line.indentationLevel()
            if (indent < parentIndent) {
                return Pair(items, currentIndex)
            }

            if (isChecklistItem(line)) {
                val (item, nextIndex) = parseChecklistItem(lines, currentIndex)
                items.add(item)
                currentIndex = nextIndex
            } else {
                currentIndex++
            }
        }

        return Pair(items, lines.size)
    }


    private fun processMarkdownReferences(content: String, plansDir: File): String {
        val referencePattern = Regex("""\[.*?\]\((.*?)(?:\s.*?)?\)""")
        val processedFiles = mutableSetOf<String>()
        val maxDepth = 3 // Reduced max depth
        
        fun processReference(referencePath: String, depth: Int): String {
            if (depth >= maxDepth) {
                return "\n<!-- Max reference depth ($maxDepth) reached -->\n"
            }
            
            val referenceFile = File(plansDir, referencePath)
            if (!referenceFile.exists() || referenceFile.extension != "md") {
                return "[$referencePath]($referencePath)"
            }
            
            val absolutePath = referenceFile.absolutePath
            if (absolutePath in processedFiles) {
                return "\n<!-- Circular reference detected to $referencePath -->\n"
            }
            
            processedFiles.add(absolutePath)
            
            return try {
                if (referenceFile.nameWithoutExtension.endsWith("_checklist")) {
                    referenceFile.readText()
                } else {
                    val fileContent = referenceFile.readText()
                    // TODO: fix missmatch with later subplan parsing
                    val summary = """
                        |<!-- Referenced Plan: ${referenceFile.nameWithoutExtension} -->
                        |<!-- Status: In Progress -->
                        |
                        |$fileContent
                    """.trimMargin()
                    
                    // Process nested references
                    referencePattern.replace(summary) { match ->
                        val nestedPath = match.groupValues[1].trim()
                        if (nestedPath.isBlank()) match.value
                        else processReference(nestedPath, depth + 1)
                    }
                }
            } catch (e: Exception) {
                "\n<!-- Error reading referenced file: ${e.message} -->\n"
            } finally {
                processedFiles.remove(absolutePath)
            }
        }
        
        return try {
            // Process top-level references
            referencePattern.replace(content) { match ->
                val path = match.groupValues[1].trim()
                if (path.isBlank()) match.value
                else processReference(path, 0)
            }
        } catch (e: Exception) {
            """
            |<!-- Error processing markdown references: ${e.message} -->
            |$content
            """.trimMargin()
        }
    }

    fun getContextFilesForPlans(files: List<FileData>): List<FileData> {
        return project.service<AiderPlanPromptService>().filterPlanRelevantFiles(files)
            .filter { it.filePath.endsWith("_context.yaml") }
            .flatMap { file ->
                val contextFile = File(file.filePath)
                parseContextYaml(contextFile).mapNotNull {
                    val filePath = File(project.basePath, it.filePath)
                    if (filePath.exists()) {
                        FileData(filePath.absolutePath, it.isReadOnly)
                    } else null
                }

            }
    }

    private val plansDir = File(project.basePath, AIDER_PLANS_FOLDER)

    fun getAiderPlans(): List<AiderPlan> {
        if (!this.plansDir.exists()) {
            this.plansDir.mkdir()
        }

        val filesToConsider: List<File> = this.plansDir.listFiles()?.toList() ?: listOf<File>()
        return getAiderPlans(filesToConsider)
    }
    // TODO 01.06.2025 pwegner: extend to also consider cost file (if available) => archive plan will should move this file too
    fun loadPlanFromFile(file: File): AiderPlan? {
        return try {
            val content = file.readText()
            if (!content.contains(AIDER_PLAN_MARKER)) return null

            // Extract plan content from the original content
            val planContent = content.substringAfter(AIDER_PLAN_MARKER).trim()

            // Process markdown references to include referenced content for checklist extraction
            val expandedContent = processMarkdownReferences(content, plansDir)

            // Get checklist items from expanded content
            val planChecklist = extractChecklistItems(expandedContent)

            // Look for associated checklist file
            val checklistFile = File(plansDir, "${file.nameWithoutExtension}_checklist.md")
            val checklistItems = if (checklistFile.exists()) {
                val checklistContent = checklistFile.readText()
                if (checklistContent.contains(AIDER_PLAN_CHECKLIST_MARKER)) {
                    // Process references in checklist file too
                    val expandedChecklistContent = processMarkdownReferences(checklistContent, plansDir)
                    extractChecklistItems(expandedChecklistContent)
                } else emptyList()
            } else emptyList()

            // Combine checklist items from both files
            val combinedChecklist = (checklistItems + planChecklist).distinctBy {
                it.description.trim()
            }

            // Include plan, checklist and context files in the files list
            val files = mutableListOf(FileData(file.absolutePath, false))
            if (checklistFile.exists()) {
                files.add(FileData(checklistFile.absolutePath, false))
            }

            // Look for associated context file
            val contextFile = File(plansDir, "${file.nameWithoutExtension}_context.yaml")
            val contextFiles = if (contextFile.exists()) {
                files.add(FileData(contextFile.absolutePath, false))
                parseContextYaml(contextFile)
            } else emptyList()

            // Extract subplan references from the original content
            val subplanRefs = extractSubplanReferences(content)
            val subplans = subplanRefs.mapNotNull { subplanRef ->
                val subplanFile = File(plansDir, subplanRef)
                if (subplanFile.exists()) loadPlanFromFile(subplanFile) else null
            }

            AiderPlan(
                plan = planContent,
                checklist = combinedChecklist,
                planFiles = files,
                contextFiles = contextFiles,
                childPlans = subplans
            )
        } catch (e: Exception) {
            println("Error processing plan file ${file.name}: ${e.message}")
            null
        }
    }

    fun getAiderPlans(filesInPlanFolder: List<File>): List<AiderPlan> {
        // First load all plans without hierarchy
        val allPlans = filesInPlanFolder
            .filter { file -> file.extension == "md" && !file.nameWithoutExtension.endsWith("_checklist") }
            .mapNotNull { file -> loadPlanFromFile(file) }
            .toList()

        // Create a map to track all plans by their path
        val plansMap = mutableMapOf<String, AiderPlan>()
        
        // First pass: Create all plans without relationships
        allPlans.forEach { plan ->
            plansMap[plan.mainPlanFile?.filePath ?: ""] = plan
        }
        
        // Second pass: Establish parent-child relationships
        allPlans.forEach { plan ->
            val content = File(plan.mainPlanFile?.filePath ?: "").readText()
            val references = extractSubplanReferences(content)
            
            // Get all referenced plans as children, regardless of whether they have their own subplans
            val childPlans = references.mapNotNull { referencePath ->
                val absolutePath = File(plansDir, referencePath).absolutePath
                plansMap[absolutePath]?.let { childPlan ->
                    // Create a new copy of the child plan with this plan as its parent
                    childPlan.copy(parentPlan = plan)
                }
            }
            
            // Always update the plan with its children, even if the children list is empty
            plansMap[plan.mainPlanFile?.filePath ?: ""] = plan.copy(
                childPlans = childPlans
            )
            
            // Update each child plan in the map with its parent reference
            childPlans.forEach { childPlan ->
                plansMap[childPlan.mainPlanFile?.filePath ?: ""] = childPlan
            }
        }
        
        // Return only root plans (those without parents)
        return plansMap.values.filter { it.parentPlan == null }
    }


    private fun String.indentationLevel(): Int =
        indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: length

    private fun isChecklistItem(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.matches(Regex("""^-?\s*\[([ xX])\].*"""))
    }

    private fun parseChecklistItem(
        lines: List<String>,
        currentIndex: Int
    ): Pair<ChecklistItem, Int> {
        val line = lines[currentIndex].trim()
        val indent = lines[currentIndex].indentationLevel()

        // Extract checkbox state and description
        val checkboxMatch = Regex("""^-?\s*\[([ xX])\](.*)""").find(line)
        if (checkboxMatch != null) {
            val (checkState, description) = checkboxMatch.destructured
            val checked = checkState.trim().uppercase() in setOf("X", "✓")

            // Look for nested items
            var nextIndex = currentIndex + 1
            val children = if (nextIndex < lines.size && hasNestedItems(lines, nextIndex, indent)) {
                val (nestedItems, newIndex) = parseChecklistItems(lines, nextIndex, indent)
                nextIndex = newIndex
                nestedItems
            } else emptyList()

            return Pair(ChecklistItem(description.trim(), checked, children), nextIndex)
        }

        // Fallback for malformed items
        return Pair(ChecklistItem(line, false, emptyList()), currentIndex + 1)
    }

    private fun hasNestedItems(
        lines: List<String>,
        currentIndex: Int,
        parentIndent: Int
    ): Boolean {
        val nextLine = lines.getOrNull(currentIndex)?.takeIf { it.isNotBlank() } ?: return false
        return nextLine.indentationLevel() > parentIndent && isChecklistItem(nextLine)
    }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String =
        project.service<AiderPlanPromptService>().createAiderPlanSystemPrompt(commandData)


    private fun extractSubplanReferences(content: String): List<String> {
        val subplans = mutableListOf<String>()
        
        // First try to find subplans using the structured format
        val structuredPattern = Regex("""${SUBPLAN_START_MARKER}.*?\n.*?\((.*?)\)""", RegexOption.DOT_MATCHES_ALL)
        structuredPattern.findAll(content).forEach { match ->
            val path = match.groupValues[1].trim()
            if (path.isNotBlank()) {
                subplans.add(path)
            }
        }
        
        // Then look for regular markdown links that might be subplans
        val linkPattern = Regex("""\[.*?\]\((.*?)(?:\s.*?)?\)""")
        linkPattern.findAll(content).forEach { match ->
            val path = match.groupValues[1].trim()
            if (path.isNotBlank() && path.endsWith(".md") && !path.endsWith("_checklist.md")) {
                subplans.add(path)
            }
        }
        
        return subplans.distinct()
    }

    private fun parseContextYaml(contextFile: File): List<FileData> =
        ContextFileHandler.readContextFile(contextFile, project.basePath.toString())

}
