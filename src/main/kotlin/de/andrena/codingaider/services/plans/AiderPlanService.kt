package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.model.ContextFileHandler
import de.andrena.codingaider.services.AiderIgnoreService
import java.io.File
import java.time.LocalDateTime


@Service(Service.Level.PROJECT)
class AiderPlanService(private val project: Project, private val aiderIgnoreService: AiderIgnoreService) {

    init {
        println("AiderPlanService initialized with project: ${project.basePath}")
    }

    constructor(project: Project) : this(project, project.service<AiderIgnoreService>())
    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLAN_CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        const val DEFAULT_AIDER_PLANS_FOLDER = ".coding-aider-plans"
        const val AIDER_PLANS_FOLDER = DEFAULT_AIDER_PLANS_FOLDER // Keep for backward compatibility
        const val FINISHED_AIDER_PLANS_FOLDER = ".coding-aider-plans-finished"
        const val STRUCTURED_MODE_MARKER = "[STRUCTURED MODE]"
        const val SUBPLAN_START_MARKER = "<!-- SUBPLAN:"
        const val SUBPLAN_END_MARKER = "<!-- END_SUBPLAN -->"
    }

    fun getAiderPlansFolder(): String {
        val settings = de.andrena.codingaider.settings.AiderProjectSettings.getInstance(project)
        println("AiderProjectSettings instance: $settings")
        return settings.plansFolderPath ?: DEFAULT_AIDER_PLANS_FOLDER
    }

    fun createPlanFolderIfNeeded(commandData: CommandData) {
        if (commandData.structuredMode) {
            val plansDir = File(commandData.projectPath, getAiderPlansFolder())
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
        service<PlanExecutionCostService>().recordInitialPlanCreation(costData, commandData)
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
        val aiderIgnoreService = project.service<AiderIgnoreService>()
        return project.service<AiderPlanPromptService>().filterPlanRelevantFiles(files)
            .filter { it.filePath.endsWith("_context.yaml") }
            .flatMap { file ->
                val contextFile = File(file.filePath)
                parseContextYaml(contextFile).mapNotNull {
                    val filePath = File(project.basePath, it.filePath)
                    if (filePath.exists() && !aiderIgnoreService.isIgnored(filePath.absolutePath)) {
                        FileData(filePath.absolutePath, it.isReadOnly)
                    } else null
                }

            }
    }

    fun getAiderPlans(): List<AiderPlan> {
        val plansDir = File(project.basePath, getAiderPlansFolder())
        if (!plansDir.exists()) {
            plansDir.mkdir()
        }

        val filesToConsider: List<File> = plansDir.listFiles()?.toList() ?: listOf<File>()
        return getAiderPlans(filesToConsider)
    }
    fun loadPlanFromFile(file: File): AiderPlan? {
        return try {
            val content = file.readText()
            if (!content.contains(AIDER_PLAN_MARKER)) return null

            // Detect if this is a single-file plan by checking for embedded checklist marker
            val isSingleFileFormat = content.contains(AIDER_PLAN_CHECKLIST_MARKER)
            
            if (isSingleFileFormat) {
                loadSingleFilePlan(file, content)
            } else {
                loadMultiFilePlan(file, content)
            }
        } catch (e: Exception) {
            println("Error processing plan file ${file.name}: ${e.message}")
            null
        }
    }
    
    private fun loadSingleFilePlan(file: File, content: String): AiderPlan? {
        // Extract plan content from the original content (before checklist marker)
        val planContent = content.substringAfter(AIDER_PLAN_MARKER).substringBefore(AIDER_PLAN_CHECKLIST_MARKER).trim()

        // Process markdown references to include referenced content for checklist extraction
        val plansDir = File(project.basePath, getAiderPlansFolder())
        val expandedContent = processMarkdownReferences(content, plansDir)

        // Extract embedded checklist items
        val checklistContent = extractEmbeddedChecklistContent(expandedContent)
        val combinedChecklist = extractChecklistItems(checklistContent)

        // Extract embedded context files
        val embeddedContextFiles = extractEmbeddedContextFiles(content)

        // Include only the main plan file
        val files = mutableListOf<FileData>()
        
        // Add main plan file if not ignored
        if (!aiderIgnoreService.isIgnored(file.absolutePath)) {
            files.add(FileData(file.absolutePath, false))
        }

        // Look for associated history file
        val historyFile = File(plansDir, "${file.nameWithoutExtension}_history.md")
        if (historyFile.exists() && !aiderIgnoreService.isIgnored(historyFile.absolutePath)) {
            files.add(FileData(historyFile.absolutePath, false))
        }

        // Extract subplan references from the original content
        val subplanRefs = extractSubplanReferences(content)
        val subplans = subplanRefs.mapNotNull { subplanRef ->
            val subplanFile = File(plansDir, subplanRef)
            if (subplanFile.exists()) loadPlanFromFile(subplanFile) else null
        }

        return AiderPlan(
            plan = planContent,
            checklist = combinedChecklist,
            planFiles = files,
            contextFiles = embeddedContextFiles,
            childPlans = subplans,
            isSingleFileFormat = true
        )
    }

    private fun loadMultiFilePlan(file: File, content: String): AiderPlan? {
        // Extract plan content from the original content
        val planContent = content.substringAfter(AIDER_PLAN_MARKER).trim()

        // Process markdown references to include referenced content for checklist extraction
        val plansDir = File(project.basePath, getAiderPlansFolder())
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

        // Include plan, checklist and context files in the files list (filtering with aiderignore)
        val files = mutableListOf<FileData>()
        
        // Add main plan file if not ignored
        if (!aiderIgnoreService.isIgnored(file.absolutePath)) {
            files.add(FileData(file.absolutePath, false))
        }
        
        // Add checklist file if exists and not ignored
        if (checklistFile.exists() && !aiderIgnoreService.isIgnored(checklistFile.absolutePath)) {
            files.add(FileData(checklistFile.absolutePath, false))
        }

        // Look for associated context file
        val contextFile = File(plansDir, "${file.nameWithoutExtension}_context.yaml")
        val contextFiles = if (contextFile.exists() && !aiderIgnoreService.isIgnored(contextFile.absolutePath)) {
            files.add(FileData(contextFile.absolutePath, false))
            parseContextYaml(contextFile)
        } else emptyList()

        // Look for associated history file
        val historyFile = File(plansDir, "${file.nameWithoutExtension}_history.md")
        if (historyFile.exists() && !aiderIgnoreService.isIgnored(historyFile.absolutePath)) {
            files.add(FileData(historyFile.absolutePath, false))
        }

        // Extract subplan references from the original content
        val subplanRefs = extractSubplanReferences(content)
        val subplans = subplanRefs.mapNotNull { subplanRef ->
            val subplanFile = File(plansDir, subplanRef)
            if (subplanFile.exists()) loadPlanFromFile(subplanFile) else null
        }

        return AiderPlan(
            plan = planContent,
            checklist = combinedChecklist,
            planFiles = files,
            contextFiles = contextFiles,
            childPlans = subplans,
            isSingleFileFormat = false
        )
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
        val plansDir = File(project.basePath, getAiderPlansFolder())
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

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val promptService = project.service<AiderPlanPromptService>()
        val activePlanService = project.service<ActivePlanService>()
        
        // Check if we're executing a subplan
        val activePlan = activePlanService.getActivePlan()
        val currentSubplan = activePlanService.getCurrentSubplan()
        
        if (activePlan != null && currentSubplan != null && currentSubplan != activePlan) {
            // We're executing a specific subplan
            return promptService.createSubplanExecutionPrompt(activePlan, currentSubplan, commandData)
        }
        
        // Default plan execution
        return promptService.createAiderPlanSystemPrompt(commandData)
    }


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

    private fun extractEmbeddedChecklistContent(content: String): String {
        val checklistStart = content.indexOf(AIDER_PLAN_CHECKLIST_MARKER)
        if (checklistStart == -1) return ""
        
        // Find the end of the checklist section (before next major section or embedded YAML)
        val checklistContent = content.substring(checklistStart + AIDER_PLAN_CHECKLIST_MARKER.length)
        
        // Look for the start of Implementation Context section or end of file
        val contextSectionStart = checklistContent.indexOf("## Implementation Context")
        val yamlStart = checklistContent.indexOf("```yaml")
        
        val endIndex = when {
            contextSectionStart != -1 && yamlStart != -1 -> minOf(contextSectionStart, yamlStart)
            contextSectionStart != -1 -> contextSectionStart
            yamlStart != -1 -> yamlStart
            else -> checklistContent.length
        }
        
        return checklistContent.substring(0, endIndex).trim()
    }

    private fun extractEmbeddedContextFiles(content: String): List<FileData> {
        val yamlBlockPattern = Regex("""```yaml\s*\n(.*?)\n```""", RegexOption.DOT_MATCHES_ALL)
        val matches = yamlBlockPattern.findAll(content)
        
        for (match in matches) {
            val yamlContent = match.groupValues[1]
            if (yamlContent.contains("files:") || yamlContent.contains("path:")) {
                return try {
                    // Create a temporary file to parse the YAML content
                    val tempFile = kotlin.io.path.createTempFile(suffix = ".yaml").toFile()
                    tempFile.writeText("---\n$yamlContent")
                    val result = parseContextYaml(tempFile)
                    tempFile.delete()
                    result
                } catch (e: Exception) {
                    println("Error parsing embedded YAML context: ${e.message}")
                    emptyList()
                }
            }
        }
        
        return emptyList()
    }

}
