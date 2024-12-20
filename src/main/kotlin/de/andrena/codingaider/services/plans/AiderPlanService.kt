package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.model.ContextFileHandler
import java.io.File


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
        val planRefs = mutableMapOf<String, AiderPlan>()
        val maxDepth = 5 // Limit recursion depth
        val processedContent = StringBuilder()
        
        fun processReferences(text: String, currentFile: String, depth: Int = 0) {
            if (depth >= maxDepth) {
                processedContent.append("\n<!-- Max reference depth ($maxDepth) reached -->")
                return
            }
            
            if (currentFile in processedFiles) {
                processedContent.append("\n<!-- Circular reference detected to $currentFile -->")
                return
            }
            
            processedFiles.add(currentFile)
            
            var lastIndex = 0
            referencePattern.findAll(text).forEach { matchResult ->
                // Add text before the match
                processedContent.append(text.substring(lastIndex, matchResult.range.first))
                
                try {
                    val referencePath = matchResult.groupValues[1].trim()
                    if (referencePath.isBlank()) {
                        processedContent.append(matchResult.value)
                    } else {
                        val referenceFile = File(plansDir, referencePath)
                        if (!referenceFile.exists() || referenceFile.extension != "md") {
                            processedContent.append(matchResult.value)
                        } else {
                            val absolutePath = referenceFile.absolutePath
                            if (!referenceFile.nameWithoutExtension.endsWith("_checklist")) {
                                val referencedPlan = planRefs.getOrPut(absolutePath) {
                                    loadPlanFromFile(referenceFile) ?: return@forEach
                                }
                                
                                val referencedContent = referenceFile.readText().takeIf { it.isNotBlank() } 
                                    ?: return@forEach
                                
                                processedContent.append("""
                                    |<!-- Referenced Plan: ${referenceFile.nameWithoutExtension} -->
                                    |<!-- Status: ${if (referencedPlan.isPlanComplete()) "Complete" else "In Progress"} -->
                                    |<!-- Progress: ${referencedPlan.totalChecklistItems() - referencedPlan.openChecklistItems().size}/${referencedPlan.totalChecklistItems()} items completed -->
                                    |
                                """.trimMargin())
                                
                                processReferences(referencedContent, absolutePath, depth + 1)
                            } else {
                                try {
                                    processedContent.append("\n${referenceFile.readText()}\n")
                                } catch (e: Exception) {
                                    processedContent.append("\n<!-- Error reading checklist file: ${e.message} -->\n")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    processedContent.append("\n<!-- Error processing reference: ${e.message} -->\n")
                }
                lastIndex = matchResult.range.last + 1
            }
            
            // Add remaining text
            if (lastIndex < text.length) {
                processedContent.append(text.substring(lastIndex))
            }
            
            processedFiles.remove(currentFile)
        }
        
        return try {
            processReferences(content, "root", 0)
            processedContent.toString()
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

    fun loadPlanFromFile(file: File): AiderPlan? {
        return try {
            val content = file.readText()
            if (!content.contains(AIDER_PLAN_MARKER)) return null

            // Process markdown references to include referenced content
            val expandedContent = processMarkdownReferences(content, plansDir)

            // Extract plan content from the expanded content
            val planContent = expandedContent.substringAfter(AIDER_PLAN_MARKER).trim()

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

            AiderPlan(
                plan = planContent,
                checklist = combinedChecklist,
                planFiles = files,
                contextFiles = contextFiles
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
            .toMutableList()

        // Then establish parent-child relationships based on references
        allPlans.forEach { plan ->
            val content = File(plan.mainPlanFile?.filePath ?: return@forEach).readText()
            val referencePattern = Regex("""\[.*?\]\((.*?)(?:\s.*?)?\)""")
            
            referencePattern.findAll(content).forEach { matchResult ->
                val referencePath = matchResult.groupValues[1]
                val referenceFile = File(plansDir, referencePath)
                
                if (referenceFile.exists() && !referenceFile.nameWithoutExtension.endsWith("_checklist")) {
                    val referencedPlan = allPlans.find { it.mainPlanFile?.filePath == referenceFile.absolutePath }
                    if (referencedPlan != null) {
                        // Update parent-child relationships
                        val updatedChildPlan = referencedPlan.copy(parentPlan = plan)
                        val updatedParentPlan = plan.copy(
                            childPlans = plan.childPlans + updatedChildPlan
                        )
                        
                        // Replace plans in list with updated versions
                        allPlans[allPlans.indexOf(plan)] = updatedParentPlan
                        allPlans[allPlans.indexOf(referencedPlan)] = updatedChildPlan
                    }
                }
            }
        }

        // Return only root plans (those without parents)
        return allPlans.filter { it.parentPlan == null }
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

    private fun parseContextYaml(contextFile: File): List<FileData> =
        ContextFileHandler.readContextFile(contextFile, project.basePath.toString())

}
