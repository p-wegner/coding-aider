package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
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

    private fun parseChecklistItems(lines: List<String>, startIndex: Int, parentIndent: Int): Pair<List<ChecklistItem>, Int> {
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
        return content.replace(referencePattern) { matchResult ->
            val referencePath = matchResult.groupValues[1]
            val referenceFile = File(plansDir, referencePath)
            if (referenceFile.exists() && referenceFile.extension == "md") {
                "\n${referenceFile.readText()}\n"
            } else {
                matchResult.value
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
                    } else null
                } catch (e: Exception) {
                    println("Error processing plan file ${file.name}: ${e.message}")
                    null
                }
            } ?: emptyList()
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
        val files = commandData.files

        val existingPlan = getExistingPlans(files)

        val s = """
            SYSTEM Instead of making changes to the code, markdown files should be used to track progress on the feature.
            SYSTEM A plan consists of three files:
            SYSTEM 1. A detailed description of the requested feature
            SYSTEM 2. A separate file with a checklist for tracking the progress
            SYSTEM 3. A context.yaml file listing all relevant files needed for implementing the plan
            SYSTEM The file should be saved in the $AIDER_PLANS_FOLDER directory in the project.
            SYSTEM Always start plans with the line $AIDER_PLAN_MARKER and checklists with $AIDER_PLAN_CHECKLIST_MARKER at the beginning of the file and use this marker in existing files to identify plans and checklists.
            SYSTEM The plan should focus on high level descriptions of the requested features and major implementation details.
            SYSTEM The checklist should focus on the required implementation steps on a more fine grained level.
            SYSTEM The three files should be named consistently:
            SYSTEM - feature_name.md (plan)
            SYSTEM - feature_name_checklist.md (checklist)
            SYSTEM - feature_name_context.yaml (file list)
            SYSTEM The plan and checklist should reference each other using markdown file references.
            SYSTEM The context.yaml should list all files that will be needed to implement the plan.
            SYSTEM The context.yaml must follow this format:
            SYSTEM ---
            SYSTEM files:
            SYSTEM - path: "full/path/to/file"
            SYSTEM   readOnly: false
            SYSTEM - path: "full/path/to/another/file"
            SYSTEM   readOnly: true
            SYSTEM Be sure to use correct relative path (same folder) references between the files.
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

    private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    private data class ContextYamlFile(val path: String, val readOnly: Boolean = false)
    private data class ContextYamlData(val files: List<ContextYamlFile> = emptyList())

    private fun parseContextYaml(contextFile: File): List<FileData> {
        return try {
            val yamlData: ContextYamlData = objectMapper.readValue(contextFile)
            yamlData.files.map { FileData(it.path, it.readOnly) }
        } catch (e: Exception) {
            println("Error parsing context yaml ${contextFile.name}: ${e.message}")
            emptyList()
        }
    }

}
