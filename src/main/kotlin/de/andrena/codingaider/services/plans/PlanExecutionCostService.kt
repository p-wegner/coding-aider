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
            
            // Extract model information - handle multiple formats
            val modelRegex = listOf(
                Regex(">\\s*Model:\\s*([^\\n]+?)(?:\\s+with\\s+|\\s*\\n)"),  // Standard format
                Regex("Model:\\s*([^\\n]+?)(?:\\s+with\\s+|\\s*\\n)"),       // Without leading >
                Regex(">\\s*Using model:\\s*([^\\n]+)")                      // Alternative format
            )
            
            for (regex in modelRegex) {
                regex.find(output)?.let {
                    val extractedModel = it.groupValues[1].trim()
                    if (extractedModel.isNotEmpty()) {
                        model = extractedModel
                        break
                    }
                }
            }
            
            // Extract token counts - get the last occurrence, handle international formats
            val tokensRegex = listOf(
                Regex("Tokens:\\s*(\\d+(?:[\\.,]\\d+)?[k]?)\\s*sent,\\s*(\\d+(?:[\\.,]\\d+)?[k]?)\\s*received"),
                Regex("(\\d+(?:[\\.,]\\d+)?[k]?)\\s*sent,\\s*(\\d+(?:[\\.,]\\d+)?[k]?)\\s*received")
            )
            
            for (regex in tokensRegex) {
                regex.findAll(output).lastOrNull()?.let {
                    tokensSent = parseTokenCount(it.groupValues[1])
                    tokensReceived = parseTokenCount(it.groupValues[2])
                    break
                }
            }
            
            // Extract cost information - get the last occurrence, handle international formats
            val costRegex = listOf(
                Regex("Cost:\\s*\\$(\\d+[\\.,]\\d+)\\s*message,\\s*\\$(\\d+[\\.,]\\d+)\\s*session"),
                Regex("\\$(\\d+[\\.,]\\d+)\\s*message,\\s*\\$(\\d+[\\.,]\\d+)\\s*session")
            )
            
            for (regex in costRegex) {
                regex.findAll(output).lastOrNull()?.let {
                    messageCost = it.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
                    sessionCost = it.groupValues[2].replace(",", ".").toDoubleOrNull() ?: 0.0
                    break
                }
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
                    // Handle international number formats by replacing comma with dot
                    val normalizedStr = tokenStr.substring(0, tokenStr.length - 1).replace(",", ".")
                    val value = normalizedStr.toDoubleOrNull() ?: 0.0
                    (value * 1000).toInt()
                }
                else -> {
                    // Handle international number formats by replacing comma with dot
                    val normalizedStr = tokenStr.replace(",", ".")
                    normalizedStr.toIntOrNull() ?: 0
                }
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
    private val costChangeListeners = mutableListOf<(String) -> Unit>()
    
    companion object {
        private const val HISTORY_FILE_SUFFIX = "_history.md"
        private const val EXECUTION_ENTRY_PREFIX = "<!-- EXECUTION_ENTRY: "
        private const val EXECUTION_ENTRY_SUFFIX = " -->"
        private const val EXECUTION_HISTORY_START = "<!-- EXECUTION_HISTORY_START -->"
        private const val EXECUTION_HISTORY_END = "<!-- EXECUTION_HISTORY_END -->"
    }
    
    fun recordExecutionCost(plan: AiderPlan, commandOutput: String, commandData: CommandData) {
        try {
            val costData = ExecutionCostData.fromCommandOutput(commandOutput)
            val planId = plan.mainPlanFile?.filePath ?: return
            
            if (!executionHistoryCache.containsKey(planId)) {
                executionHistoryCache[planId] = mutableListOf()
            }
            
            // Add the new execution cost data
            executionHistoryCache[planId]?.add(costData)
            
            // Update the history file with the new execution and updated totals
            updateHistoryFile(plan, costData, commandData)
            
            // Notify listeners that cost data has changed for this plan
            notifyCostChanged(planId)
            
            // Log the total cost for this plan
            val totalCost = getTotalCost(planId)
            val totalTokens = getTotalTokens(planId)
            logger.info("Plan $planId: Total cost so far: $${String.format("%.4f", totalCost)}, Total tokens: $totalTokens")
        } catch (e: Exception) {
            logger.warn("Failed to record execution cost", e)
        }
    }
    
    fun addCostChangeListener(listener: (String) -> Unit) {
        costChangeListeners.add(listener)
    }
    
    fun removeCostChangeListener(listener: (String) -> Unit) {
        costChangeListeners.remove(listener)
    }
    
    private fun notifyCostChanged(planId: String) {
        costChangeListeners.forEach { it(planId) }
    }
    
    fun getExecutionHistory(planId: String): List<ExecutionCostData> {
        return executionHistoryCache[planId] ?: loadHistoryFromFile(planId)
    }
    fun getTotalCost(planId: String): Double {
        return getExecutionHistory(planId).sumOf { it.sessionCost }
    }
    
    fun getTotalTokens(planId: String): Int {
        return getExecutionHistory(planId).sumOf { it.tokensSent + it.tokensReceived }
    }
    
    private fun updateHistoryFile(plan: AiderPlan, costData: ExecutionCostData, commandData: CommandData) {
        val planFile = plan.mainPlanFile?.filePath ?: return
        val historyFile = File(planFile.replace(".md", HISTORY_FILE_SUFFIX))
        
        // Create history file if it doesn't exist
        if (!historyFile.exists()) {
            createHistoryFile(historyFile, plan)
        }
        
        // Create the structured entry
        val structuredEntry = "$EXECUTION_ENTRY_PREFIX${costData.timestamp}|${costData.model}|${costData.tokensSent}|${costData.tokensReceived}|${costData.messageCost}|${costData.sessionCost}|${costData.summary.replace("\n", "\\n")}$EXECUTION_ENTRY_SUFFIX"
        
        // Create the human-readable entry
        val humanReadableEntry = createHistoryEntry(costData, commandData)
        
        // If the file exists, update the structured data section
        if (historyFile.exists()) {
            val content = historyFile.readText()
            
            if (content.contains(EXECUTION_HISTORY_START) && content.contains(EXECUTION_HISTORY_END)) {
                // Find the position of the markers
                val startPos = content.indexOf(EXECUTION_HISTORY_START) + EXECUTION_HISTORY_START.length
                val endPos = content.indexOf(EXECUTION_HISTORY_END)
                
                if (startPos < endPos) {
                    // Insert the new entry after the start marker
                    val newContent = StringBuilder(content)
                    newContent.insert(startPos + 1, structuredEntry + "\n")
                    
                    // Append the human-readable part at the end of the file
                    if (!newContent.endsWith("\n\n")) {
                        newContent.append(if (newContent.endsWith("\n")) "\n" else "\n\n")
                    }
                    newContent.append(humanReadableEntry)
                    
                    historyFile.writeText(newContent.toString())
                } else {
                    // Markers found but in wrong order, recreate the file
                    recreateHistoryFile(historyFile, plan, costData, commandData)
                }
            } else {
                // Markers not found, recreate the file
                recreateHistoryFile(historyFile, plan, costData, commandData)
            }
        } else {
            // File doesn't exist, create it
            createHistoryFile(historyFile, plan)
            
            // Append the entries
            val content = StringBuilder(historyFile.readText())
            content.insert(
                content.indexOf(EXECUTION_HISTORY_END), 
                structuredEntry + "\n"
            )
            content.append("\n\n").append(humanReadableEntry)
            
            historyFile.writeText(content.toString())
        }
        
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
            |
            |$EXECUTION_HISTORY_START
            |<!-- Format: timestamp|model|tokensSent|tokensReceived|messageCost|sessionCost|summary -->
            |$EXECUTION_HISTORY_END
            |""".trimMargin()
        
        historyFile.writeText(header)
    }
    
    /**
     * Recreates the history file with proper structure
     */
    private fun recreateHistoryFile(historyFile: File, plan: AiderPlan, costData: ExecutionCostData, commandData: CommandData) {
        // First, try to extract any existing entries
        val existingEntries = mutableListOf<ExecutionCostData>()
        if (historyFile.exists()) {
            val content = historyFile.readText()
            val entryPattern = Regex("$EXECUTION_ENTRY_PREFIX([^$EXECUTION_ENTRY_SUFFIX]+)$EXECUTION_ENTRY_SUFFIX")
            
            entryPattern.findAll(content).forEach { matchResult ->
                try {
                    val parts = matchResult.groupValues[1].split("|")
                    if (parts.size >= 6) {
                        val timestamp = try {
                            LocalDateTime.parse(parts[0].trim())
                        } catch (e: Exception) {
                            LocalDateTime.now()
                        }
                        
                        existingEntries.add(
                            ExecutionCostData(
                                timestamp,
                                parts[2].toIntOrNull() ?: 0,
                                parts[3].toIntOrNull() ?: 0,
                                parts[4].toDoubleOrNull() ?: 0.0,
                                parts[5].toDoubleOrNull() ?: 0.0,
                                parts[1],
                                if (parts.size > 6) parts[6].replace("\\n", "\n") else ""
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse existing entry", e)
                }
            }
        }
        
        // Create a new file
        createHistoryFile(historyFile, plan)
        
        // Add all existing entries
        val content = StringBuilder(historyFile.readText())
        existingEntries.forEach { entry ->
            val structuredEntry = "$EXECUTION_ENTRY_PREFIX${entry.timestamp}|${entry.model}|${entry.tokensSent}|${entry.tokensReceived}|${entry.messageCost}|${entry.sessionCost}|${entry.summary.replace("\n", "\\n")}$EXECUTION_ENTRY_SUFFIX"
            content.insert(
                content.indexOf(EXECUTION_HISTORY_END),
                structuredEntry + "\n"
            )
            
            // Add human-readable entry
            content.append("\n\n").append(createHistoryEntry(entry, commandData))
        }
        
        // Add the new entry
        val structuredEntry = "$EXECUTION_ENTRY_PREFIX${costData.timestamp}|${costData.model}|${costData.tokensSent}|${costData.tokensReceived}|${costData.messageCost}|${costData.sessionCost}|${costData.summary.replace("\n", "\\n")}$EXECUTION_ENTRY_SUFFIX"
        content.insert(
            content.indexOf(EXECUTION_HISTORY_END),
            structuredEntry + "\n"
        )
        
        // Add human-readable entry for the new data
        content.append("\n\n").append(createHistoryEntry(costData, commandData))
        
        historyFile.writeText(content.toString())
    }
    
    /**
     * Creates a history entry for an execution
     */
    private fun createHistoryEntry(costData: ExecutionCostData, commandData: CommandData): String {
        val timestamp = costData.getFormattedTimestamp()
        
        // Format token counts with k suffix for thousands
        val sentTokens = if (costData.tokensSent >= 1000) 
            String.format("%,dk", costData.tokensSent / 1000) 
        else 
            costData.tokensSent.toString()
            
        val receivedTokens = if (costData.tokensReceived >= 1000) 
            String.format("%,dk", costData.tokensReceived / 1000) 
        else 
            costData.tokensReceived.toString()
        
        // Get total cost information for the plan
        val planId = commandData.files.firstOrNull { it.filePath.endsWith(".md") && !it.filePath.endsWith("_checklist.md") }?.filePath
        val totalCostInfo = if (planId != null) {
            // Get the updated execution history including the current execution
            val allExecutions = if (executionHistoryCache.containsKey(planId)) {
                executionHistoryCache[planId] ?: emptyList()
            } else {
                loadHistoryFromFile(planId) + listOf(costData)
            }
            
            // Calculate totals directly from the execution data
            val totalCost = allExecutions.sumOf { it.sessionCost }
            val totalTokens = allExecutions.sumOf { it.tokensSent + it.tokensReceived }
            
            """| Total Cost (All Executions) | \$${String.format("%.4f", totalCost)} |
            || Total Tokens (All Executions) | ${if (totalTokens >= 1000) String.format("%,dk", totalTokens / 1000) else totalTokens} |"""
        } else ""
        
        return """### Execution on $timestamp
            |
            || Metric | Value |
            || ------ | ----- |
            || Model | ${costData.model} |
            || Tokens Sent | $sentTokens |
            || Tokens Received | $receivedTokens |
            || Message Cost | \$${String.format("%.4f", costData.messageCost)} |
            || Session Cost | \$${String.format("%.4f", costData.sessionCost)} |
            |$totalCostInfo
            |
            |${if (costData.summary.isNotBlank()) "**Summary:**\n${costData.summary}" else ""}
            |""".trimMargin()
    }
    
    private fun loadHistoryFromFile(planId: String): List<ExecutionCostData> {
        try {
            val historyFile = File(planId.replace(".md", HISTORY_FILE_SUFFIX))
            if (!historyFile.exists()) {
                return emptyList()
            }
            
            val content = historyFile.readText()
            val executionEntries = mutableListOf<ExecutionCostData>()
            
            // Parse structured entries using regex with constant markers
            val entryPattern = Regex("$EXECUTION_ENTRY_PREFIX([^|]+)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)(?:\\|([^$EXECUTION_ENTRY_SUFFIX]*))$EXECUTION_ENTRY_SUFFIX")
            
            entryPattern.findAll(content).forEach { matchResult ->
                try {
                    // Handle cases with and without summary
                    val groups = matchResult.groupValues
                    val timestampStr = groups[1].trim()
                    val model = groups[2].trim()
                    val tokensSentStr = groups[3].trim()
                    val tokensReceivedStr = groups[4].trim()
                    val messageCostStr = groups[5].trim()
                    val sessionCostStr = groups[6].trim()
                    val summaryEncoded = if (groups.size > 7) groups[7] else ""
                    
                    // Parse timestamp
                    val timestamp = try {
                        LocalDateTime.parse(timestampStr)
                    } catch (e: Exception) {
                        logger.warn("Failed to parse timestamp: $timestampStr", e)
                        LocalDateTime.now() // Fallback if parsing fails
                    }
                    
                    // Parse numeric values with international format support
                    val tokensSent = tokensSentStr.toIntOrNull() ?: 0
                    val tokensReceived = tokensReceivedStr.toIntOrNull() ?: 0
                    val messageCost = messageCostStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val sessionCost = sessionCostStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    
                    // Decode summary (replace \n with actual newlines)
                    val summary = summaryEncoded.replace("\\n", "\n")
                    
                    executionEntries.add(
                        ExecutionCostData(
                            timestamp,
                            tokensSent,
                            tokensReceived,
                            messageCost,
                            sessionCost,
                            model,
                            summary
                        )
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to parse execution entry: ${matchResult.value}", e)
                }
            }
            
            // If no structured entries found, try to parse from the table format as fallback
            if (executionEntries.isEmpty()) {
                val fallbackPattern = Regex("### Execution on ([^\n]+).*?Model \\| ([^\n|]+).*?Tokens Sent \\| ([^\n|]+).*?Tokens Received \\| ([^\n|]+).*?Message Cost \\| \\$([^\n|]+).*?Session Cost \\| \\$([^\n|]+)", RegexOption.DOT_MATCHES_ALL)
                
                fallbackPattern.findAll(content).forEach { matchResult ->
                    try {
                        val (timestampStr, model, tokensSentStr, tokensReceivedStr, messageCostStr, sessionCostStr) = matchResult.destructured
                        
                        // Parse timestamp (format: yyyy-MM-dd HH:mm:ss)
                        val timestamp = try {
                            LocalDateTime.parse(
                                timestampStr.trim(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                        } catch (e: Exception) {
                            LocalDateTime.now() // Fallback if parsing fails
                        }
                        
                        // Parse token values (handle k suffix)
                        val tokensSent = parseTokenCount(tokensSentStr.trim())
                        val tokensReceived = parseTokenCount(tokensReceivedStr.trim())
                        
                        // Parse cost values with international format support
                        val messageCost = messageCostStr.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                        val sessionCost = sessionCostStr.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                        
                        executionEntries.add(
                            ExecutionCostData(
                                timestamp,
                                tokensSent,
                                tokensReceived,
                                messageCost,
                                sessionCost,
                                model.trim(),
                                ""
                            )
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to parse execution entry using fallback method", e)
                    }
                }
            }
            
            // Cache the loaded entries
            if (executionEntries.isNotEmpty()) {
                executionHistoryCache[planId] = executionEntries.toMutableList()
            }
            
            return executionEntries
        } catch (e: Exception) {
            logger.warn("Failed to load history from file for plan: $planId", e)
            return emptyList()
        }
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
