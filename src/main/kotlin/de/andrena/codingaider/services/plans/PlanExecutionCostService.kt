package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.command.CommandData
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Data class to store execution cost information
 */
data class ExecutionCostData(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val tokensSent: Int = 0,
    val tokensReceived: Int = 0,
    val messageCost: Double = 0.0,
    val sessionCost: Double = 0.0,
    val model: String = "",
    val summary: String = ""
) {
    fun getFormattedTimestamp(): String {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }
    
    fun getTotalCost(): Double = sessionCost
    
    fun getTotalTokens(): Int = tokensSent + tokensReceived
    
    companion object {
        fun fromCommandOutput(output: String): ExecutionCostData {
            // Default values
            var tokensSent = 0
            var tokensReceived = 0
            var messageCost = 0.0
            var sessionCost = 0.0
            var model = ""
            var summary = ""
            
            // Extract model information
            val modelRegex = Regex(">\\s*Model:\\s*([^\\n]+)")
            modelRegex.find(output)?.let {
                model = it.groupValues[1].trim()
            }
            
            // Extract token counts - get the last occurrence
            val tokensRegex = Regex("Tokens:\\s*(\\d+(?:\\.\\d+)?[k]?)\\s*sent,\\s*(\\d+(?:\\.\\d+)?[k]?)\\s*received")
            tokensRegex.findAll(output).lastOrNull()?.let {
                tokensSent = parseTokenCount(it.groupValues[1])
                tokensReceived = parseTokenCount(it.groupValues[2])
            }
            
            // Extract cost information - get the last occurrence
            val costRegex = Regex("Cost:\\s*\\$(\\d+\\.\\d+)\\s*message,\\s*\\$(\\d+\\.\\d+)\\s*session")
            costRegex.findAll(output).lastOrNull()?.let {
                messageCost = it.groupValues[1].toDoubleOrNull() ?: 0.0
                sessionCost = it.groupValues[2].toDoubleOrNull() ?: 0.0
            }
            
            // Extract summary if available
            val summaryRegex = Regex("<aider-summary>([\\s\\S]*?)</aider-summary>")
            summaryRegex.find(output)?.let {
                summary = it.groupValues[1].trim()
            }
            
            return ExecutionCostData(
                LocalDateTime.now(),
                tokensSent,
                tokensReceived,
                messageCost,
                sessionCost,
                model,
                summary
            )
        }
        
        private fun parseTokenCount(tokenStr: String): Int {
            return when {
                tokenStr.endsWith("k") -> {
                    val value = tokenStr.substring(0, tokenStr.length - 1).toDoubleOrNull() ?: 0.0
                    (value * 1000).toInt()
                }
                else -> tokenStr.toIntOrNull() ?: 0
            }
        }
    }
}

/**
 * Service for tracking plan execution costs
 */
@Service(Service.Level.APP)
class PlanExecutionCostService() {
    private val logger = Logger.getInstance(PlanExecutionCostService::class.java)
    private val executionHistoryCache = mutableMapOf<String, MutableList<ExecutionCostData>>()
    
    companion object {
        private const val HISTORY_FILE_SUFFIX = "_history.md"
    }
    
    /**
     * Records execution cost for a plan
     */
    fun recordExecutionCost(plan: AiderPlan, commandOutput: String, commandData: CommandData) {
        try {
            val costData = ExecutionCostData.fromCommandOutput(commandOutput)
            val planId = plan.mainPlanFile?.filePath ?: return
            
            // Add to in-memory cache
            if (!executionHistoryCache.containsKey(planId)) {
                executionHistoryCache[planId] = mutableListOf()
            }
            executionHistoryCache[planId]?.add(costData)
            
            // Update history file
            updateHistoryFile(plan, costData, commandData)
        } catch (e: Exception) {
            logger.warn("Failed to record execution cost", e)
        }
    }
    
    /**
     * Gets the latest execution cost for a plan
     */
    fun getLatestExecutionCost(planId: String): ExecutionCostData? {
        return executionHistoryCache[planId]?.lastOrNull()
    }
    
    /**
     * Gets all execution costs for a plan
     */
    fun getExecutionHistory(planId: String): List<ExecutionCostData> {
        return executionHistoryCache[planId] ?: loadHistoryFromFile(planId)
    }
    
    /**
     * Gets the total cost for a plan
     */
    fun getTotalCost(planId: String): Double {
        return getExecutionHistory(planId).sumOf { it.getTotalCost() }
    }
    
    /**
     * Gets the total tokens used for a plan
     */
    fun getTotalTokens(planId: String): Int {
        return getExecutionHistory(planId).sumOf { it.getTotalTokens() }
    }
    
    /**
     * Updates the history file for a plan
     */
    private fun updateHistoryFile(plan: AiderPlan, costData: ExecutionCostData, commandData: CommandData) {
        val planFile = plan.mainPlanFile?.filePath ?: return
        val historyFile = File(planFile.replace(".md", HISTORY_FILE_SUFFIX))
        
        // Create history file if it doesn't exist
        if (!historyFile.exists()) {
            createHistoryFile(historyFile, plan)
        }
        
        // Append execution record
        val historyEntry = createHistoryEntry(costData, commandData)
        historyFile.appendText("\n\n$historyEntry")
        
        // Refresh file in IDE
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(historyFile)
    }
    
    /**
     * Creates a new history file
     */
    private fun createHistoryFile(historyFile: File, plan: AiderPlan) {
        val planName = plan.mainPlanFile?.filePath?.let { File(it).nameWithoutExtension } ?: "Unknown Plan"
        val header = """# Execution History: $planName
            |
            |This file tracks the execution history and costs for the plan.
            |
            |## Executions
            |""".trimMargin()
        
        historyFile.writeText(header)
    }
    
    /**
     * Creates a history entry for an execution
     */
    private fun createHistoryEntry(costData: ExecutionCostData, commandData: CommandData): String {
        val timestamp = costData.getFormattedTimestamp()
        val message = commandData.message.take(100).let { if (it.length < commandData.message.length) "$it..." else it }
        
        // Format token counts with k suffix for thousands
        val sentTokens = if (costData.tokensSent >= 1000) 
            String.format("%.1fk", costData.tokensSent / 1000.0) 
        else 
            costData.tokensSent.toString()
            
        val receivedTokens = if (costData.tokensReceived >= 1000) 
            String.format("%.1fk", costData.tokensReceived / 1000.0) 
        else 
            costData.tokensReceived.toString()
        
        return """### Execution on $timestamp
            |
            |**Model:** ${costData.model}
            |**Tokens:** $sentTokens sent, $receivedTokens received
            |**Cost:** \$${String.format("%.4f", costData.messageCost)} message, \$${String.format("%.4f", costData.sessionCost)} session
            |
            |**Prompt:**
            |```
            |$message
            |```
            |
            |${if (costData.summary.isNotBlank()) "**Summary:**\n${costData.summary}" else ""}
            |""".trimMargin()
    }
    
    private fun loadHistoryFromFile(planId: String): List<ExecutionCostData> {
        // This is a placeholder for future implementation
        // Would parse the history file and extract execution records
        // TODO 03.05.2025 pwegner: implement

        return emptyList()
    }
}
