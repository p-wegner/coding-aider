package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.AiderHistoryService
import de.andrena.codingaider.services.RunningCommandService
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JOptionPane

@Service(Service.Level.PROJECT)
class PostActionPlanCreationService(private val project: Project) {
    
    private val planService = project.service<AiderPlanService>()
    private val historyService = project.service<AiderHistoryService>()
    
    /**
     * Creates a plan from the last completed command
     * @return true if plan was created successfully, false otherwise
     */
    fun createPlanFromLastCommand(): Boolean {
        val runningCommandService = project.service<RunningCommandService>()
        val lastCommand = runningCommandService.getLastCompletedCommand() ?: return false
        val lastOutput = runningCommandService.getLastCommandOutput() ?: return false
        
        return createPlanFromCommand(lastCommand, lastOutput)
    }
    
    /**
     * Creates a plan from a specific command and its output
     * @param commandData The command data
     * @param commandOutput The command output
     * @return true if plan was created successfully, false otherwise
     */
    fun createPlanFromCommand(commandData: CommandData, commandOutput: String): Boolean {
        try {
            // Generate plan name based on command
            val planName = generatePlanName(commandData.message)
            
            // Create plan directory if it doesn't exist
            val plansDir = File(project.basePath, AiderPlanService.AIDER_PLANS_FOLDER)
            if (!plansDir.exists()) {
                plansDir.mkdir()
            }
            
            // Create plan files
            createPlanFiles(planName, commandData, commandOutput)
            
            return true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Error creating plan: ${e.message}",
                "Plan Creation Error",
                JOptionPane.ERROR_MESSAGE
            )
            return false
        }
    }
    
    private fun generatePlanName(message: String): String {
        // Extract key words from message to create a plan name
        val words = message.split(" ")
            .filter { it.length > 3 }
            .take(3)
            .joinToString("_") { it.lowercase() }
            .replace(Regex("[^a-z0-9_]"), "")
        
        // Add timestamp to ensure uniqueness
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return if (words.isNotEmpty()) "${words}_$timestamp" else "plan_$timestamp"
    }
    
    private fun createPlanFiles(planName: String, commandData: CommandData, commandOutput: String) {
        val plansDir = File(project.basePath, AiderPlanService.AIDER_PLANS_FOLDER)
        
        // Create main plan file
        val planFile = File(plansDir, "$planName.md")
        planFile.writeText(createPlanContent(planName, commandData, commandOutput))
        
        // Create checklist file
        val checklistFile = File(plansDir, "${planName}_checklist.md")
        checklistFile.writeText(createChecklistContent(planName, commandData, commandOutput))
        
        // Create context file
        val contextFile = File(plansDir, "${planName}_context.yaml")
        contextFile.writeText(createContextContent(commandData))
    }
    
    private fun createPlanContent(planName: String, commandData: CommandData, commandOutput: String): String {
        val title = commandData.message.split(" ")
            .filter { it.length > 2 }
            .joinToString(" ") { it.capitalize() }
            .take(50)
        
        return """
            [Coding Aider Plan]
            
            ## Overview
            ${title.ifEmpty { "Implementation of requested changes" }}
            
            ## Problem Description
            ${commandData.message}
            
            ## Goals
            1. Implement the requested changes
            2. Ensure code quality and maintainability
            3. Verify functionality works as expected
            
            ## Implementation Details
            ### Changes Made
            ${extractChangesFromOutput(commandOutput)}
            
            ### Remaining Work
            ${identifyRemainingWork(commandOutput)}
            
            ## Additional Notes and Constraints
            - Created from completed Aider action on ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
            - Original command: ${commandData.message}
            
            ## References
            ${commandData.files.joinToString("\n") { "- ${it.filePath}" }}
        """.trimIndent()
    }
    
    private fun createChecklistContent(planName: String, commandData: CommandData, commandOutput: String): String {
        return """
            [Coding Aider Plan - Checklist]
            
            ## Completed Tasks
            - [x] Initial implementation
            
            ## Remaining Tasks
            ${generateChecklistItems(commandOutput)}
            
            ## Testing
            - [ ] Test the implemented functionality
            - [ ] Verify all requirements are met
            - [ ] Check for edge cases
        """.trimIndent()
    }
    
    private fun createContextContent(commandData: CommandData): String {
        val files = commandData.files.joinToString("\n") { fileData ->
            """
            - path: "${fileData.filePath}"
              readOnly: ${fileData.isReadOnly}
            """.trimIndent()
        }
        
        return """
            ---
            files:
            $files
        """.trimIndent()
    }
    
    private fun extractChangesFromOutput(output: String): String {
        // Extract changes from the output
        val changes = output.lines()
            .filter { it.contains("<<<<<<< SEARCH") || it.contains(">>>>>>> REPLACE") }
            .joinToString("\n") { it.trim() }
        
        if (changes.isNotEmpty()) {
            return "The following changes were made:\n\n```\n$changes\n```"
        }
        
        // Look for intention and summary blocks
        val intentionMatch = Regex("<aider-intention>(.*?)</aider-intention>", RegexOption.DOT_MATCHES_ALL)
            .find(output)?.groupValues?.get(1)?.trim()
        
        val summaryMatch = Regex("<aider-summary>(.*?)</aider-summary>", RegexOption.DOT_MATCHES_ALL)
            .find(output)?.groupValues?.get(1)?.trim()
        
        return buildString {
            intentionMatch?.let { append("**Intention:**\n$it\n\n") }
            summaryMatch?.let { append("**Summary:**\n$it\n\n") }
            if (intentionMatch == null && summaryMatch == null) {
                append("Changes were made based on the command, but no structured summary was available.")
            }
        }
    }
    
    private fun identifyRemainingWork(output: String): String {
        // Look for TODO comments or mentions of future work
        val todoPattern = Regex("(?i)(todo|future work|next steps|remaining|follow-up).*")
        val todos = output.lines()
            .filter { todoPattern.containsMatchIn(it) }
            .joinToString("\n") { "- ${it.trim()}" }
        
        return if (todos.isNotEmpty()) {
            todos
        } else {
            "No specific remaining work identified from the command output."
        }
    }
    
    private fun generateChecklistItems(output: String): String {
        // Generate checklist items from the output
        val todoPattern = Regex("(?i)(todo|future work|next steps|remaining|follow-up).*")
        val todos = output.lines()
            .filter { todoPattern.containsMatchIn(it) }
            .joinToString("\n") { "- [ ] ${it.trim().replace(Regex("(?i)^(todo|future work|next steps|remaining|follow-up):?\\s*"), "")}" }
        
        return if (todos.isNotEmpty()) {
            todos
        } else {
            "- [ ] Review and test the implemented changes\n- [ ] Document the changes if necessary"
        }
    }
}
