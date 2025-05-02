package de.andrena.codingaider.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.plans.AiderPlanService
import java.io.File
import java.util.regex.Pattern

/**
 * Service responsible for creating plans from completed Aider commands.
 * This service handles the conversion of command outputs into structured plans.
 */
@Service(Service.Level.PROJECT)
class PostActionPlanCreationService(private val project: Project) {
    
    companion object {
        private const val PLANS_FOLDER = AiderPlanService.Companion.AIDER_PLANS_FOLDER
    }
    
    /**
     * Creates a plan from a specific command and its output
     * @param commandData The command data
     * @param commandOutput The command output
     * @return true if plan was created successfully, false otherwise
     */
    fun createPlanFromCommand(commandData: CommandData, commandOutput: String): Boolean {
        try {
            val plansDir = File(project.basePath, PLANS_FOLDER)
            if (!plansDir.exists()) {
                plansDir.mkdir()
            }
            val planCreationCommand = createPlanCreationCommand(commandData, commandOutput)
            val executor = IDEBasedExecutor(project, planCreationCommand)
            executor.execute()

            return true
        } catch (e: Exception) {
            notifyPlanCreationFailure("Error creating plan: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    private fun createPlanCreationCommand(commandData: CommandData, commandOutput: String): CommandData {
        val planMessage = buildString {
            append("Create a plan based on this completed request: ${commandData.message}\n\n")
            append("The action made the following changes:\n")
            
            val summary = extractSummaryFromOutput(commandOutput)
            if (summary.isNotEmpty()) {
                append(summary)
            } else {
                // TODO 02.05.2025 pwegner: decide what to do in this case
                val truncatedOutput = commandOutput.take(1000) + (if (commandOutput.length > 1000) "..." else "")
                append(truncatedOutput)
            }
            
            append("\n\nCreate a plan to implement the request that incorporates the changes that were already made and includes any needed follow-up tasks.")
        }
        
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
