package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.ChecklistItem
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service(Service.Level.PROJECT)
class PostActionPlanCreationService(private val project: Project) {
    
    companion object {
        private const val PLAN_MARKER = "[Coding Aider Plan]"
        private const val CHECKLIST_MARKER = "[Coding Aider Plan - Checklist]"
        private const val PLANS_FOLDER = ".coding-aider-plans"
    }
    
    /**
     * Creates a plan from a completed command and its output
     * 
     * @param commandData The command data that was executed
     * @param output The output from the command execution
     * @return true if plan creation was successful, false otherwise
     */
    fun createPlanFromCommand(commandData: CommandData, output: String): Boolean {
        try {
            // Create plans directory if it doesn't exist
            val plansDir = File(project.basePath, PLANS_FOLDER)
            if (!plansDir.exists()) {
                plansDir.mkdir()
            }
            
            // Generate plan name based on command message
            val planName = generatePlanName(commandData.message)
            
            // Create plan files
            createMainPlanFile(plansDir, planName, commandData, output)
            createChecklistFile(plansDir, planName, output)
            createContextFile(plansDir, planName, commandData.files)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
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
            appendLine("This plan was automatically generated from a completed Aider action.")
            appendLine("The original command was: `${commandData.message}`")
            appendLine()
            appendLine("## Goals")
            appendLine("1. Complete the implementation started by the action")
            appendLine("2. Address any follow-up tasks identified during execution")
            appendLine()
            appendLine("## Additional Notes and Constraints")
            appendLine("- Original command was executed with model: ${commandData.llm}")
            appendLine("- Command was executed on ${LocalDateTime.now()}")
            appendLine()
            appendLine("## Original Command Output")
            appendLine("```")
            appendLine(output.take(2000)) // Limit output size
            if (output.length > 2000) {
                appendLine("... (output truncated)")
            }
            appendLine("```")
            appendLine()
            appendLine("## Implementation Strategy")
            appendLine("Continue with the implementation based on the completed action.")
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
            if (tasks.isNotEmpty()) {
                tasks.forEach { task ->
                    appendLine("- [ ] $task")
                }
            } else {
                appendLine("- [ ] Complete implementation")
                appendLine("- [ ] Test the implementation")
                appendLine("- [ ] Update documentation if needed")
            }
            appendLine()
            appendLine("## Dependencies")
            appendLine("- Original command execution")
        }
        
        checklistFile.writeText(checklistContent)
        return checklistFile
    }
    
    private fun createContextFile(plansDir: File, planName: String, files: List<FileData>): File {
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
        
        output.lines().forEach { line ->
            for (pattern in taskPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    val task = match.groupValues[1].trim()
                    if (task.length > 10 && !tasks.contains(task)) {
                        tasks.add(task)
                    }
                }
            }
        }
        
        return tasks.take(10) // Limit to 10 tasks to avoid overwhelming the user
    }
}
