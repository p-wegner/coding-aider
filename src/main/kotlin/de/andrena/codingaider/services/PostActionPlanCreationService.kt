package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.services.plans.AiderPlanService
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

@Service(Service.Level.PROJECT)
class PostActionPlanCreationService(private val project: Project) {
    
    companion object {
        private const val PLANS_FOLDER = AiderPlanService.AIDER_PLANS_FOLDER
        private const val PLAN_MARKER = AiderPlanService.AIDER_PLAN_MARKER
        private const val CHECKLIST_MARKER = AiderPlanService.AIDER_PLAN_CHECKLIST_MARKER
    }
    
    fun createPlanFromCommand(commandData: CommandData, output: String): Boolean {
        try {
            // Create plans directory if it doesn't exist
            val plansDir = File(project.basePath, PLANS_FOLDER)
            if (!plansDir.exists()) {
                plansDir.mkdir()
            }
            
            // Generate plan name based on command message
            val planName = generatePlanName(commandData.message)
            
            // Create the main plan file
            val planFile = createMainPlanFile(plansDir, planName, commandData, output)
            
            // Create the checklist file
            val checklistFile = createChecklistFile(plansDir, planName, output)
            
            // Create the context file with the files used in the command
            val contextFile = createContextFile(plansDir, planName, commandData.files)
            
            // Notify the user that the plan was created
            notifyPlanCreation(planName)
            
            return planFile.exists() && checklistFile.exists() && contextFile.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun notifyPlanCreation(planName: String) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("Coding Aider Notifications")
            .createNotification(
                "Plan Created",
                "Plan '$planName' was created from a completed command",
                com.intellij.notification.NotificationType.INFORMATION
            )
            .notify(project)
    }
    
    private fun generatePlanName(message: String): String {
        // Create a plan name from the message
        val baseName = message.trim()
            .take(40)
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")
        
        // Add timestamp to ensure uniqueness
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "${baseName}_${timestamp}"
    }
    
    private fun createMainPlanFile(plansDir: File, planName: String, commandData: CommandData, output: String): File {
        val planFile = File(plansDir, "$planName.md")
        
        val planContent = buildString {
            appendLine(PLAN_MARKER)
            appendLine()
            appendLine("## Overview")
            appendLine("Plan created from completed action: ${commandData.message}")
            appendLine()
            appendLine("## Problem Description")
            appendLine("This plan was automatically generated from a completed Aider action. The original command was:")
            appendLine("```")
            appendLine(commandData.message)
            appendLine("```")
            appendLine()
            appendLine("## Goals")
            appendLine("1. Document the changes made by the completed action")
            appendLine("2. Track any follow-up tasks identified during implementation")
            appendLine("3. Provide a structured approach for continuing work on this feature")
            appendLine()
            appendLine("## Additional Notes and Constraints")
            appendLine("- This plan was created from a completed action on ${LocalDateTime.now()}")
            appendLine("- The original command used the ${commandData.llm} model")
            appendLine("- ${commandData.files.size} files were included in the original context")
            if (commandData.additionalArgs.isNotEmpty()) {
                appendLine("- Additional arguments: ${commandData.additionalArgs}")
            }
            appendLine()
            appendLine("## Implementation Summary")
            appendLine("### Completed Changes")
            
            // Extract summary from output if available
            val summary = extractSummaryFromOutput(output)
            if (summary.isNotEmpty()) {
                appendLine(summary)
            } else {
                appendLine("The action made the following changes:")
                appendLine("```")
                appendLine(output.take(500) + (if (output.length > 500) "..." else ""))
                appendLine("```")
            }
            
            appendLine()
            appendLine("## References")
            appendLine("- [Checklist](${planName}_checklist.md)")
            appendLine("- [Context](${planName}_context.yaml)")
        }
        
        planFile.writeText(planContent)
        return planFile
    }
    
    private fun createChecklistFile(plansDir: File, planName: String, output: String): File {
        val checklistFile = File(plansDir, "${planName}_checklist.md")
        
        // Extract potential tasks from the output
        val tasks = extractTasksFromOutput(output)
        
        val checklistContent = buildString {
            appendLine(CHECKLIST_MARKER)
            appendLine()
            appendLine("## Main Tasks")
            appendLine("- [x] Complete initial implementation")
            
            if (tasks.isNotEmpty()) {
                appendLine()
                appendLine("## Follow-up Tasks")
                tasks.forEach { task ->
                    appendLine("- [ ] $task")
                }
            } else {
                appendLine("- [ ] Review the implementation for any issues")
                appendLine("- [ ] Add tests if needed")
                appendLine("- [ ] Update documentation")
            }
            
            appendLine()
            appendLine("## Review")
            appendLine("- [ ] Code review")
            appendLine("- [ ] Test coverage")
            appendLine("- [ ] Documentation completeness")
        }
        
        checklistFile.writeText(checklistContent)
        return checklistFile
    }
    
    private fun createContextFile(plansDir: File, planName: String, files: List<de.andrena.codingaider.command.FileData>): File {
        val contextFile = File(plansDir, "${planName}_context.yaml")
        
        val contextContent = buildString {
            appendLine("---")
            appendLine("files:")
            files.forEach { file ->
                appendLine("- path: \"${file.filePath}\"")
                appendLine("  readOnly: ${file.isReadOnly}")
            }
        }
        
        contextFile.writeText(contextContent)
        return contextFile
    }
    
    private fun extractTasksFromOutput(output: String): List<String> {
        val tasks = mutableListOf<String>()
        
        // Look for potential tasks in the output
        val taskPatterns = listOf(
            Regex("(?:TODO|FIXME|Next steps?|Follow-up|We should)[:;]\\s*(.+)"),
            Regex("(?:need to|should|could|must)\\s+(.+?)(?:\\.|$)"),
            Regex("- \\[ \\]\\s*(.+)")  // Markdown checklist items
        )
        
        for (pattern in taskPatterns) {
            pattern.findAll(output).forEach { matchResult ->
                val task = matchResult.groupValues[1].trim()
                if (task.isNotEmpty() && task.length < 100) {  // Avoid overly long "tasks"
                    tasks.add(task)
                }
            }
        }
        
        // Also look for tasks in XML summary blocks if present
        extractTasksFromSummaryBlock(output)?.let { tasks.addAll(it) }
        
        return tasks.distinct().take(10)  // Limit to 10 tasks to avoid overwhelming the user
    }
    
    private fun extractTasksFromSummaryBlock(output: String): List<String>? {
        val summaryPattern = Pattern.compile("<aider-summary>(.*?)</aider-summary>", Pattern.DOTALL)
        val matcher = summaryPattern.matcher(output)
        
        if (matcher.find()) {
            val summaryContent = matcher.group(1).trim()
            val tasks = mutableListOf<String>()
            
            // Look for bullet points or numbered lists in the summary
            val bulletPattern = Regex("^\\s*[-*]\\s+(.+)$", RegexOption.MULTILINE)
            bulletPattern.findAll(summaryContent).forEach { 
                tasks.add(it.groupValues[1].trim())
            }
            
            val numberedPattern = Regex("^\\s*\\d+\\.\\s+(.+)$", RegexOption.MULTILINE)
            numberedPattern.findAll(summaryContent).forEach {
                tasks.add(it.groupValues[1].trim())
            }
            
            return tasks.distinct()
        }
        
        return null
    }
    
    private fun extractSummaryFromOutput(output: String): String {
        // Try to extract the summary from XML tags if present
        val intentionPattern = Pattern.compile("<aider-intention>(.*?)</aider-intention>", Pattern.DOTALL)
        val summaryPattern = Pattern.compile("<aider-summary>(.*?)</aider-summary>", Pattern.DOTALL)
        
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
