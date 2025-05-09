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
            
            // > Model: claude-3-5-haiku-20241022 with diff edit format
            val modelRegex = Regex("(?:>\\s*)?(?:Model|Using model):\\s*([^\\n]+?)(?:\\s+with\\s+|\\s*$)")
            
            modelRegex.find(output)?.let {
                val extractedModel = it.groupValues[1].trim()
                if (extractedModel.isNotEmpty()) {
                    model = extractedModel
                }
            }
            
            // > Tokens: 7.2k sent, 1.3k received. Cost: $0.01 message, $0.01 session.
            // > Tokens: 6.8k sent, 999 received. Cost: $0.0094 message, $0.0094 session.
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
        private const val EXECUTION_HISTORY_START = "<!-- EXECUTION_HISTORY_START -->"
        private const val EXECUTION_HISTORY_END = "<!-- EXECUTION_HISTORY_END -->"
        private const val EXECUTION_DATA_HEADER = "<!-- timestamp,model,tokensSent,tokensReceived,messageCost,sessionCost,summary -->"
        private const val EXECUTION_DATA_PREFIX = "<!-- EXEC_DATA: "
        private const val EXECUTION_DATA_SUFFIX = " -->"
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
        
        // Create the structured CSV-style entry
        val escapedSummary = costData.summary.replace("\n", "\\n").replace(",", "\\,")
        val structuredEntry = "$EXECUTION_DATA_PREFIX${costData.timestamp},${costData.model},${costData.tokensSent},${costData.tokensReceived},${costData.messageCost},${costData.sessionCost},$escapedSummary$EXECUTION_DATA_SUFFIX"
        
        // If the file exists, update the structured data section
        if (historyFile.exists()) {
            val content = historyFile.readText()
            
            if (content.contains(EXECUTION_HISTORY_START) && content.contains(EXECUTION_HISTORY_END)) {
                // Find the position of the markers
                val startPos = content.indexOf(EXECUTION_HISTORY_START) + EXECUTION_HISTORY_START.length
                val endPos = content.indexOf(EXECUTION_HISTORY_END)
                
                if (startPos < endPos) {
                    // Insert the new entry after the start marker and header
                    val headerPos = content.indexOf(EXECUTION_DATA_HEADER, startPos)
                    val insertPos = if (headerPos > startPos && headerPos < endPos) {
                        headerPos + EXECUTION_DATA_HEADER.length
                    } else {
                        startPos
                    }
                    
                    val newContent = StringBuilder(content)
                    
                    // If header doesn't exist, add it
                    if (headerPos == -1 || headerPos > endPos) {
                        newContent.insert(startPos + 1, EXECUTION_DATA_HEADER + "\n")
                        newContent.insert(startPos + EXECUTION_DATA_HEADER.length + 2, structuredEntry + "\n")
                    } else {
                        newContent.insert(insertPos + 1, structuredEntry + "\n")
                    }
                    
                    // Update the human-readable table
                    updateHumanReadableTable(newContent, plan)
                    
                    historyFile.writeText(newContent.toString())
                } else {
                    // Markers found but in wrong order, recreate the file
                    recreateHistoryFile(historyFile, plan, costData)
                }
            } else {
                // Markers not found, recreate the file
                recreateHistoryFile(historyFile, plan, costData)
            }
        } else {
            // File doesn't exist, create it
            createHistoryFile(historyFile, plan)
            
            // Append the entries
            val content = StringBuilder(historyFile.readText())
            content.insert(
                content.indexOf(EXECUTION_HISTORY_END), 
                EXECUTION_DATA_HEADER + "\n" + structuredEntry + "\n"
            )
            
            // Add human-readable table
            updateHumanReadableTable(content, plan)
            
            historyFile.writeText(content.toString())
        }
        
        // Refresh file in IDE
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(historyFile)
    }
    
    /**
     * Updates the human-readable table in the history file
     */
    private fun updateHumanReadableTable(content: StringBuilder, plan: AiderPlan) {
        // Remove existing table if present
        val tableStartPattern = "\n## Execution Summary\n"
        val tableStart = content.indexOf(tableStartPattern)
        if (tableStart != -1) {
            // Find the end of the table (next heading or end of file)
            val nextHeading = content.indexOf("\n## ", tableStart + tableStartPattern.length)
            val tableEnd = if (nextHeading != -1) nextHeading else content.length
            content.delete(tableStart, tableEnd)
        }
        
        // Get all executions for this plan
        val planId = plan.mainPlanFile?.filePath ?: return
        val executions = getExecutionHistory(planId)
        
        if (executions.isEmpty()) return
        
        // Create a new table with all executions
        val tableBuilder = StringBuilder()
        tableBuilder.append("\n## Execution Summary\n\n")
        tableBuilder.append("| Date | Model | Tokens (Sent/Received) | Cost | Notes |\n")
        tableBuilder.append("| ---- | ----- | --------------------- | ---- | ----- |\n")
        
        // Add each execution as a row
        executions.sortedByDescending { it.timestamp }.forEach { execution ->
            val date = execution.getFormattedTimestamp()
            val model = execution.model.substringAfterLast("-").take(10)
            
            // Format tokens with k suffix for thousands
            val sentTokens = if (execution.tokensSent >= 1000) 
                String.format("%,dk", execution.tokensSent / 1000) 
            else 
                execution.tokensSent.toString()
                
            val receivedTokens = if (execution.tokensReceived >= 1000) 
                String.format("%,dk", execution.tokensReceived / 1000) 
            else 
                execution.tokensReceived.toString()
            
            val tokens = "$sentTokens / $receivedTokens"
            val cost = String.format("$%.4f", execution.sessionCost)
            val notes = execution.summary.takeIf { it.isNotBlank() }?.let {
                if (it.length > 30) it.take(27) + "..." else it
            } ?: ""
            
            tableBuilder.append("| $date | $model | $tokens | $cost | $notes |\n")
        }
        
        // Add total cost information
        val totalCost = executions.sumOf { it.sessionCost }
        val totalTokensSent = executions.sumOf { it.tokensSent }
        val totalTokensReceived = executions.sumOf { it.tokensReceived }
        
        tableBuilder.append("\n**Total Cost:** $${String.format("%.4f", totalCost)} | ")
        tableBuilder.append("**Total Tokens:** ${formatTokenCount(totalTokensSent)} sent, ")
        tableBuilder.append("${formatTokenCount(totalTokensReceived)} received | ")
        tableBuilder.append("**Executions:** ${executions.size}\n")
        
        // Append the table to the content
        content.append(tableBuilder)
    }
    
    private fun formatTokenCount(tokens: Int): String {
        return if (tokens >= 1000) String.format("%,dk", tokens / 1000) else tokens.toString()
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
            |$EXECUTION_HISTORY_START
            |$EXECUTION_DATA_HEADER
            |$EXECUTION_HISTORY_END
            |""".trimMargin()
        
        historyFile.writeText(header)
    }
    
    /**
     * Recreates the history file with proper structure
     */
    private fun recreateHistoryFile(historyFile: File, plan: AiderPlan, costData: ExecutionCostData) {
        // First, try to extract any existing entries
        val existingEntries = mutableListOf<ExecutionCostData>()
        if (historyFile.exists()) {
            val content = historyFile.readText()
            
            // Try to parse both old and new format entries
            val oldEntryPattern = Regex("<!-- EXECUTION_ENTRY: ([^|]+)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)(?:\\|([^>]*))? -->")
            val newEntryPattern = Regex("$EXECUTION_DATA_PREFIX([^,]+),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*)(?:,([^>]*))$EXECUTION_DATA_SUFFIX")
            
            // Parse old format entries
            oldEntryPattern.findAll(content).forEach { matchResult ->
                try {
                    parseEntryFromRegexMatch(matchResult, '|', existingEntries)
                } catch (e: Exception) {
                    logger.warn("Failed to parse existing entry (old format)", e)
                }
            }
            
            // Parse new format entries
            newEntryPattern.findAll(content).forEach { matchResult ->
                try {
                    parseEntryFromRegexMatch(matchResult, ',', existingEntries)
                } catch (e: Exception) {
                    logger.warn("Failed to parse existing entry (new format)", e)
                }
            }
        }
        
        // Create a new file
        createHistoryFile(historyFile, plan)
        
        // Add all existing entries plus the new one
        val allEntries = existingEntries + costData
        
        // Add structured data
        val content = StringBuilder(historyFile.readText())
        content.insert(
            content.indexOf(EXECUTION_HISTORY_END),
            EXECUTION_DATA_HEADER + "\n"
        )
        
        allEntries.forEach { entry ->
            val escapedSummary = entry.summary.replace("\n", "\\n").replace(",", "\\,")
            val structuredEntry = "$EXECUTION_DATA_PREFIX${entry.timestamp},${entry.model},${entry.tokensSent},${entry.tokensReceived},${entry.messageCost},${entry.sessionCost},$escapedSummary$EXECUTION_DATA_SUFFIX"
            content.insert(
                content.indexOf(EXECUTION_HISTORY_END),
                structuredEntry + "\n"
            )
        }
        
        // Add human-readable table
        updateHumanReadableTable(content, plan)
        
        historyFile.writeText(content.toString())
    }
    
    /**
     * Helper method to parse execution data from regex match
     */
    private fun parseEntryFromRegexMatch(
        matchResult: MatchResult, 
        delimiter: Char,
        entries: MutableList<ExecutionCostData>
    ) {
        val groups = matchResult.groupValues
        if (groups.size >= 7) {
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
                LocalDateTime.now() // Fallback if parsing fails
            }
            
            // Parse numeric values with international format support
            val tokensSent = tokensSentStr.toIntOrNull() ?: 0
            val tokensReceived = tokensReceivedStr.toIntOrNull() ?: 0
            val messageCost = messageCostStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val sessionCost = sessionCostStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            
            // Decode summary (replace \n with actual newlines and \, with commas)
            val summary = summaryEncoded.replace("\\n", "\n").replace("\\,", ",")
            
            entries.add(
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
        }
    }
    
    
    private fun loadHistoryFromFile(planId: String): List<ExecutionCostData> {
        try {
            val historyFile = File(planId.replace(".md", HISTORY_FILE_SUFFIX))
            if (!historyFile.exists()) {
                return emptyList()
            }
            
            val content = historyFile.readText()
            val executionEntries = mutableListOf<ExecutionCostData>()
            
            // Try to parse both old and new format entries
            val oldEntryPattern = Regex("<!-- EXECUTION_ENTRY: ([^|]+)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)(?:\\|([^>]*))? -->")
            val newEntryPattern = Regex("$EXECUTION_DATA_PREFIX([^,]+),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*)(?:,([^>]*))$EXECUTION_DATA_SUFFIX")
            
            // Parse old format entries
            oldEntryPattern.findAll(content).forEach { matchResult ->
                try {
                    parseEntryFromRegexMatch(matchResult, '|', executionEntries)
                } catch (e: Exception) {
                    logger.warn("Failed to parse execution entry (old format)", e)
                }
            }
            
            // Parse new format entries
            newEntryPattern.findAll(content).forEach { matchResult ->
                try {
                    parseEntryFromRegexMatch(matchResult, ',', executionEntries)
                } catch (e: Exception) {
                    logger.warn("Failed to parse execution entry (new format)", e)
                }
            }
            
            // If no structured entries found, try to parse from the table format as fallback
            if (executionEntries.isEmpty()) {
                val tablePattern = Regex("\\| (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) \\| ([^|]+) \\| ([^|]+) / ([^|]+) \\| \\$([^|]+) \\|", RegexOption.MULTILINE)
                
                tablePattern.findAll(content).forEach { matchResult ->
                    try {
                        val (timestampStr, model, sentTokensStr, receivedTokensStr, costStr) = matchResult.destructured
                        
                        // Parse timestamp
                        val timestamp = try {
                            LocalDateTime.parse(
                                timestampStr.trim(),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            )
                        } catch (e: Exception) {
                            LocalDateTime.now() // Fallback if parsing fails
                        }
                        
                        // Parse token values (handle k suffix)
                        val tokensSent = parseTokenCount(sentTokensStr.trim())
                        val tokensReceived = parseTokenCount(receivedTokensStr.trim())
                        
                        // Parse cost value
                        val sessionCost = costStr.trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                        
                        executionEntries.add(
                            ExecutionCostData(
                                timestamp,
                                tokensSent,
                                tokensReceived,
                                sessionCost, // Use session cost as message cost too
                                sessionCost,
                                model.trim(),
                                ""
                            )
                        )
                    } catch (e: Exception) {
                        logger.warn("Failed to parse execution entry from table", e)
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
