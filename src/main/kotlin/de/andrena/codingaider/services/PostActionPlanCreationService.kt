package de.andrena.codingaider.services.plans

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.ChainedAiderCommand
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.RunningCommandService
import java.io.File
import java.util.regex.Pattern

/**
 * Service responsible for creating plans from completed Aider commands.
 * This service handles the conversion of command outputs into structured plans.
 */
@Service(Service.Level.PROJECT)
class PostActionPlanCreationService(private val project: Project) {
    
    companion object {
        private const val PLANS_FOLDER = AiderPlanService.AIDER_PLANS_FOLDER
    }
    
    /**
     * Creates a plan from a specific command and its output
     * @param commandData The command data
     * @param commandOutput The command output
     * @return true if plan was created successfully, false otherwise
     */
    fun createPlanFromCommand(commandData: CommandData, commandOutput: String): Boolean {
        try {
            // Create plans directory if it doesn't exist
            val plansDir = File(project.basePath, PLANS_FOLDER)
            if (!plansDir.exists()) {
                plansDir.mkdir()
            }
            
            // Create a structured mode command to generate the plan
            val planCreationCommand = createPlanCreationCommand(commandData, commandOutput)
            
            // Use the RunningCommandService to execute the command
            val commandService = project.service<RunningCommandService>()
            val result = commandService.executeChainedCommands(
                project,
                listOf(ChainedAiderCommand(planCreationCommand))
            )
            
            val success = result != null
            
            if (success) {
                notifyPlanCreation("Plan created successfully from command")
            } else {
                notifyPlanCreationFailure("Failed to create plan from command")
            }
            
            return success
        } catch (e: Exception) {
            notifyPlanCreationFailure("Error creating plan: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    private fun createPlanCreationCommand(commandData: CommandData, commandOutput: String): CommandData {
        // Create a message that describes the plan to be created
        val planMessage = buildString {
            append("Create a plan based on this completed action: ${commandData.message}\n\n")
            append("The action made the following changes:\n")
            
            // Extract summary if available, otherwise use a portion of the output
            val summary = extractSummaryFromOutput(commandOutput)
            if (summary.isNotEmpty()) {
                append(summary)
            } else {
                val truncatedOutput = commandOutput.take(1000) + (if (commandOutput.length > 1000) "..." else "")
                append(truncatedOutput)
            }
            
            append("\n\nCreate a plan that documents these changes and identifies any follow-up tasks.")
        }
        
        // Create a new command data object with structured mode enabled
        return commandData.copy(
            message = planMessage,
            aiderMode = AiderMode.STRUCTURED,
            planId = null,  // Ensure we're creating a new plan, not continuing an existing one
            options = commandData.options.copy(
                disablePresentation = false,
                autoCloseDelay = 10
            )
        )
    }
    
    private fun notifyPlanCreation(message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(
                "Plan Created",
                message,
                NotificationType.INFORMATION
            )
            .notify(project)
    }
    
    private fun notifyPlanCreationFailure(message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(
                "Plan Creation Failed",
                message,
                NotificationType.ERROR
            )
            .notify(project)
    }
    
    private fun extractSummaryFromOutput(output: String): String {
        // Try to extract the summary from XML tags if present
        val intentionPattern = "<aider-intention>(.*?)</aider-intention>".toPattern(Pattern.DOTALL)
        val summaryPattern = "<aider-summary>(.*?)</aider-summary>".toPattern(Pattern.DOTALL)
        
        val intentionMatcher = intentionPattern.matcher(output)
        val summaryMatcher = summaryPattern.matcher(output)
        
        val summary = StringBuilder()
        
        if (intentionMatcher.find()) {
            summary.append("### Intention\n")
            summary.append(intentionMatcher.group(1).trim())
            summary.append("\n\n")
        }
        
        if (summaryMatcher.find()) {
            summary.append("### Summary\n")
            summary.append(summaryMatcher.group(1).trim())
        }
        
        return summary.toString()
    }
}
