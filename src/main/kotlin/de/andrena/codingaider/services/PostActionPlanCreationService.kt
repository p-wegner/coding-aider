package de.andrena.codingaider.services.plans

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.executors.api.IDEBasedExecutor.Companion.CommandFinishedCallback
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.CommandFinishedCallback
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@Service(Service.Level.PROJECT)
class PostActionPlanCreationService(private val project: Project) {
    
    companion object {
        private const val PLANS_FOLDER = AiderPlanService.AIDER_PLANS_FOLDER
        private const val PLAN_CREATION_TIMEOUT_SECONDS = 60L
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
            
            // Execute the command and wait for completion
            val latch = CountDownLatch(1)
            var success = false

            val executor = IDEBasedExecutor(project, planCreationCommand, CommandFinishedCallback {
                override fun onCommandFinished(exitCode: Int) {
                    success = exitCode == 0
                    latch.countDown()
                }
            })
            
            // Execute in a separate thread to avoid blocking the UI
            Thread {
                executor.execute()
            }.start()
            
            // Wait for the command to complete with a timeout
            if (!latch.await(PLAN_CREATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                notifyPlanCreationFailure("Plan creation timed out after $PLAN_CREATION_TIMEOUT_SECONDS seconds")
                return false
            }
            
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
            planId = null  // Ensure we're creating a new plan, not continuing an existing one
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
