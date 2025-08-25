package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class AiderPlanPromptService(private val project: Project) {
    private val planService by lazy { project.service<AiderPlanService>() }
    private val templates by lazy { AiderPlanPromptTemplates(planService) }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        if (commandData.message.startsWith("/")) {
            return createFullPromptWithPrependedSlashCommand(commandData.message, commandData.files, commandData)
        }
        
        // For plan continuation (empty message), check if we should use original prompt
        if (commandData.message.isEmpty()) {
            val settings = service<AiderSettings>()
            if (settings.useSingleFilePlanMode) {
                val existingPlan = filterPlanRelevantFiles(commandData.files).firstOrNull()
                existingPlan?.let { planFile ->
                    val originalPrompt = extractOriginalPromptFromPlan(planFile)
                    if (originalPrompt.isNotEmpty()) {
                        return createFullPrompt(originalPrompt, commandData)
                    }
                }
            }
        }
        
        val systemPrompt = createSystemPromptContent(commandData.files, commandData.projectPath)
        return createFullPrompt(systemPrompt, commandData)
    }

    /**
     * Creates a system prompt specifically for subplan execution
     */
    fun createSubplanExecutionPrompt(
        rootPlan: AiderPlan,
        currentSubplan: AiderPlan,
        commandData: CommandData
    ): String {
        val settings = service<AiderSettings>()
        val subplanContext = createSubplanContextPrompt(rootPlan, currentSubplan, settings.useSingleFilePlanMode)
        val systemPrompt = createSystemPromptContent(commandData.files, commandData.projectPath)
        val enhancedSystemPrompt = "$systemPrompt\n\n$subplanContext"
        return createFullPrompt(enhancedSystemPrompt, commandData)
    }

    private fun createSubplanContextPrompt(
        rootPlan: AiderPlan,
        currentSubplan: AiderPlan,
        useSingleFileMode: Boolean
    ): String {
        val rootPlanName = rootPlan.mainPlanFile?.filePath?.substringAfterLast('/')?.removeSuffix(".md") ?: "Root Plan"
        val subplanName = currentSubplan.mainPlanFile?.filePath?.substringAfterLast('/')?.removeSuffix(".md") ?: "Current Subplan"
        
        val completedSubplans = rootPlan.childPlans.filter { it.isPlanComplete() }
        val remainingSubplans = rootPlan.childPlans.filter { !it.isPlanComplete() && it != currentSubplan }
        
        return """
## SUBPLAN EXECUTION CONTEXT

You are currently working on a specific subplan within a larger plan hierarchy.

**Root Plan**: $rootPlanName
**Current Subplan**: $subplanName
**Completed Subplans**: ${completedSubplans.size}/${rootPlan.childPlans.size}

**Current Focus**: 
- Work ONLY on tasks related to the current subplan: $subplanName
- Complete the checklist items for this subplan before moving to other areas
- The root plan files are included for context, but focus implementation on the current subplan

${if (completedSubplans.isNotEmpty()) {
    "**Already Completed Subplans**: ${completedSubplans.joinToString(", ") { it.mainPlanFile?.filePath?.substringAfterLast('/')?.removeSuffix(".md") ?: "Unknown" }}"
} else ""}

${if (remainingSubplans.isNotEmpty()) {
    "**Remaining Subplans**: ${remainingSubplans.joinToString(", ") { it.mainPlanFile?.filePath?.substringAfterLast('/')?.removeSuffix(".md") ?: "Unknown" }}"
} else ""}

**Important**: Focus your implementation on the current subplan's requirements. Other subplans will be handled separately.
        """.trimIndent()
    }

    fun createPlanRefinementPrompt(plan: AiderPlan, refinementRequest: String): String {
        val settings = service<AiderSettings>()
        val subplanGuidance = if (settings.enableSubplans) {
            val guidance = if (settings.useSingleFilePlanMode) {
                templates.singleFileSubplanGuidancePrompt
            } else {
                templates.subplanGuidancePrompt
            }
            "Consider whether to use subplans for complex parts.\n$guidance"
        } else {
            templates.noSubplansGuidancePrompt
        }

        val basePrompt = createFullPlanRefinementPrompt(plan, subplanGuidance, refinementRequest, settings.useSingleFilePlanMode)

        return basePrompt
    }

    fun filterPlanMainFiles(files: List<FileData>): List<FileData> =
        filterPlanRelevantFiles(files).filter { it.filePath.endsWith(".md") && !it.filePath.endsWith("_checklist.md") }

    fun filterPlanRelevantFiles(files: List<FileData>): List<FileData> =
        files.filter {
            it.filePath.contains(planService.getAiderPlansFolder()) && !it.filePath.contains(AiderPlanService.FINISHED_AIDER_PLANS_FOLDER)
                    && (it.filePath.endsWith(".md") || it.filePath.endsWith(".yaml"))
        }

    private fun createSystemPromptContent(files: List<FileData>, projectPath: String): String {
        val settings = service<AiderSettings>()
        val existingPlan = filterPlanRelevantFiles(files)
        val basePrompt = if (settings.useSingleFilePlanMode) {
            templates.singleFilePlanFormatPrompt.trimStartingWhiteSpaces()
        } else {
            templates.planFileFormatPrompt.trimStartingWhiteSpaces()
        }
        val firstPlan = existingPlan.firstOrNull()
        val planSpecificPrompt = createNewPlanOrContinuePrompt(firstPlan, projectPath)
        return "${basePrompt}\n${planSpecificPrompt.trimStartingWhiteSpaces()}"
    }

    private fun createFullPlanRefinementPrompt(
        plan: AiderPlan,
        subplanGuidance: String,
        refinementRequest: String,
        useSingleFileMode: Boolean
    ): String {
        val structurePrompt = if (useSingleFileMode) templates.singleFilePlanStructurePrompt else templates.planFileStructurePrompt
        val formatPrompt = if (useSingleFileMode) templates.singleFilePlanFormatPrompt else templates.planFileFormatPrompt
        val fileText = if (useSingleFileMode) "plan file" else "plan files"
        
        val basePrompt = """<SystemPrompt>
    You are refining an existing plan ${plan.plan}. The plan should be extended or modified based on the refinement request. 
    Don't start the implementation until the $fileText are committed. The main goal is to modify the $fileText and not the implementation.
    $subplanGuidance
    $structurePrompt
    $formatPrompt
    </SystemPrompt>
    The refinement request is: <UserPrompt>$refinementRequest</UserPrompt>
            """.trimIndent()
        return basePrompt
    }

    private fun createFullPrompt(
        systemPrompt: String,
        commandData: CommandData
    ): String = """
<SystemPrompt>
$systemPrompt
</SystemPrompt>
$STRUCTURED_MODE_MESSAGE_MARKER ${commandData.message} $STRUCTURED_MODE_MESSAGE_END_MARKER
                   """.trimStartingWhiteSpaces()

    private fun createFullPromptWithPrependedSlashCommand(
        message: String,
        files: List<FileData>,
        commandData: CommandData
    ): String {
        // Extract the command part (everything up to the first space)
        val commandParts = message.split(" ", limit = 2)
        val command = commandParts[0]
        val remainingMessage = if (commandParts.size > 1) commandParts[1] else ""
        val systemPrompt = createSystemPromptContent(files, commandData.projectPath)
        return """$command <SystemPrompt>
    $systemPrompt
    </SystemPrompt>
    $STRUCTURED_MODE_MESSAGE_MARKER $remainingMessage $STRUCTURED_MODE_MESSAGE_END_MARKER
                   """.trimStartingWhiteSpaces()
    }

    private fun createNewPlanOrContinuePrompt(firstPlan: FileData?, projectPath: String): String {
        val settings = service<AiderSettings>()
        val relativePlanPath = firstPlan?.filePath?.removePrefix(projectPath) ?: ""
        return firstPlan
            ?.let {
                if (settings.useSingleFilePlanMode) {
                    templates.getSingleFileExistingPlanPrompt(relativePlanPath)
                        .replace("the project path", projectPath)
                } else {
                    templates.getExistingPlanPrompt(relativePlanPath)
                        .replace("the project path", projectPath)
                }
            }
            ?: if (settings.useSingleFilePlanMode) {
                templates.getSingleFileNewPlanPrompt(settings.enableSubplans)
            } else {
                templates.getNewPlanPrompt(settings.enableSubplans)
            }
    }

    private fun extractOriginalPromptFromPlan(planFile: FileData): String {
        return try {
            val content = java.io.File(planFile.filePath).readText()
            
            // Look for stored original prompt in HTML comment at end of file
            val originalPromptPattern = Regex("""<!-- ORIGINAL_PROMPT_START -->\s*(.*?)\s*<!-- ORIGINAL_PROMPT_END -->""", RegexOption.DOT_MATCHES_ALL)
            val match = originalPromptPattern.find(content)
            
            if (match != null) {
                match.groupValues[1].trim()
            } else {
                // Fallback to standard continuation prompt for single-file plans
                val relativePlanPath = planFile.filePath.removePrefix(project.basePath ?: "")
                templates.getSingleFileExistingPlanPrompt(relativePlanPath)
            }
        } catch (e: Exception) {
            println("Error extracting original prompt from plan: ${e.message}")
            ""
        }
    }

    private fun String.trimStartingWhiteSpaces() = trimIndent().trimStart { it.isWhitespace() }

    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLAN_CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        const val STRUCTURED_MODE_MESSAGE_MARKER = "<UserPrompt>"
        const val STRUCTURED_MODE_MESSAGE_END_MARKER = "</UserPrompt>"
    }
}
