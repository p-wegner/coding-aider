package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class AiderPlanPromptService(private val project: Project) {

    fun createPlanRefinementPrompt(plan: AiderPlan, refinementRequest: String): String {
        val settings = service<AiderSettings>()
        val subplanGuidance = if (settings.enableSubplans) {
            "Consider whether to use subplans for complex parts.\n${AiderPlanPromptTemplates.subplanGuidancePrompt}"
        } else {
            "Do not create subplans.\n${AiderPlanPromptTemplates.noSubplansGuidancePrompt}"
        }
        
        val basePrompt = """<SystemPrompt>
You are refining an existing plan ${plan.plan}. The plan should be extended or modified based on the refinement request. 
Don't start the implementation until the plan files are committed. The main goal is to modify the plan files and not the implementation.
$subplanGuidance
${AiderPlanPromptTemplates.planFileStructurePrompt}
${AiderPlanPromptTemplates.planFileFormatPrompt}
</SystemPrompt>
The refinement request is: <UserPrompt>$refinementRequest</UserPrompt>
        """.trimIndent()

        return basePrompt
    }

    fun createAiderPlanSystemPrompt(commandData: CommandData): String {
        val files = commandData.files
        val message = commandData.message

        // Check if message starts with a slash command
        if (message.startsWith("/")) {
            // Extract the command part (everything up to the first space)
            val commandParts = message.split(" ", limit = 2)
            val command = commandParts[0]
            val remainingMessage = if (commandParts.size > 1) commandParts[1] else ""
            
            // Create the system prompt without the command
            val systemPrompt = createSystemPromptContent(files, commandData.projectPath)
            
            // Return the command followed by the decorated prompt with the remaining message
            return """$command <SystemPrompt>
$systemPrompt
</SystemPrompt>
$STRUCTURED_MODE_MESSAGE_MARKER $remainingMessage $STRUCTURED_MODE_MESSAGE_END_MARKER
               """.trimStartingWhiteSpaces()
        }
        
        // Regular case without slash command
        val systemPrompt = createSystemPromptContent(files, commandData.projectPath)
        
        return """<SystemPrompt>
$systemPrompt
</SystemPrompt>
$STRUCTURED_MODE_MESSAGE_MARKER ${commandData.message} $STRUCTURED_MODE_MESSAGE_END_MARKER
               """.trimStartingWhiteSpaces()
    }
    
    private fun createSystemPromptContent(files: List<FileData>, projectPath: String): String {
        val existingPlan = filterPlanRelevantFiles(files)
        val basePrompt = AiderPlanPromptTemplates.planFileFormatPrompt.trimStartingWhiteSpaces()
        
        val settings = service<AiderSettings>()
        val firstPlan = existingPlan.firstOrNull()
        val relativePlanPath = firstPlan?.filePath?.removePrefix(projectPath) ?: ""
        val planSpecificPrompt = firstPlan?.let {
            AiderPlanPromptTemplates.getExistingPlanPrompt(relativePlanPath)
                .replace("the project path", projectPath)
        } ?: AiderPlanPromptTemplates.getNewPlanPrompt(settings.enableSubplans)
            
        return "${basePrompt}\n${planSpecificPrompt.trimStartingWhiteSpaces()}"
    }

    fun filterPlanRelevantFiles(files: List<FileData>): List<FileData> =
        files.filter {
            it.filePath.contains(AIDER_PLANS_FOLDER) && !it.filePath.contains(AiderPlanService.FINISHED_AIDER_PLANS_FOLDER)
                    && (it.filePath.endsWith(".md") || it.filePath.endsWith(".yaml"))
        }

    fun filterPlanMainFiles(files: List<FileData>): List<FileData> =
        filterPlanRelevantFiles(files).filter { it.filePath.endsWith(".md") && !it.filePath.endsWith("_checklist.md") }

    private fun String.trimStartingWhiteSpaces() = trimIndent().trimStart { it.isWhitespace() }
    
    companion object {
        const val AIDER_PLAN_MARKER = "[Coding Aider Plan]"
        const val AIDER_PLAN_CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        const val AIDER_PLANS_FOLDER = AiderPlanService.AIDER_PLANS_FOLDER
        const val STRUCTURED_MODE_MESSAGE_MARKER = "<UserPrompt>"
        const val STRUCTURED_MODE_MESSAGE_END_MARKER = "</UserPrompt>"
    }
}
