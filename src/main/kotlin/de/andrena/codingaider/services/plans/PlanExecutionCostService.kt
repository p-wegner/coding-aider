package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.command.CommandData
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class PlanExecutionCostService() {
    private val logger = Logger.getInstance(PlanExecutionCostService::class.java)
    // No longer using a cache as we always read from file
    private val costChangeListeners = mutableListOf<(String) -> Unit>()
    // Store pending plan creation records until we have a plan ID
    private val pendingPlanCreations = ConcurrentHashMap<String, ExecutionCostData>()

    companion object {
        private const val HISTORY_FILE_SUFFIX = "_history.md"
        private const val EXECUTION_HISTORY_START = "<!-- EXECUTION_HISTORY_START -->"
        private const val EXECUTION_HISTORY_END = "<!-- EXECUTION_HISTORY_END -->"
        private const val EXECUTION_DATA_HEADER =
            "<!-- timestamp,model,tokensSent,tokensReceived,messageCost,sessionCost,summary -->"
        private const val EXECUTION_DATA_PREFIX = "<!-- EXEC_DATA: "
        private const val EXECUTION_DATA_SUFFIX = " -->"
    }

    fun recordInitialPlanCreation(costData: ExecutionCostData, commandData: CommandData) {
        // Store the initial cost data using the command message as a temporary key
        // We'll update this with the actual plan ID once it's created
        val tempKey = commandData.message.hashCode().toString()
        pendingPlanCreations[tempKey] = costData
        logger.info("Recorded initial plan creation for command: ${commandData.message.take(50)}...")
    }

    fun recordExecutionCost(plan: AiderPlan, commandOutput: String, commandData: CommandData) {
        try {
            val costData = ExecutionCostData.fromCommandOutput(commandOutput)
            val planId = plan.mainPlanFile?.filePath ?: return
            
            // Check if this is a plan creation that we've been tracking
            val tempKey = commandData.message.hashCode().toString()
            val initialCostData = pendingPlanCreations.remove(tempKey)
            
            // If we have initial cost data and this is a new plan, merge the data
            val finalCostData = if (initialCostData != null && commandData.structuredMode) {
                // Use the timestamp from the initial creation but the token/cost data from the response
                costData.copy(
                    timestamp = initialCostData.timestamp,
                    summary = "Plan creation completed: ${costData.summary}"
                )
            } else {
                costData
            }
            
            updateHistoryFile(plan, finalCostData, commandData)
            notifyCostChanged(planId)
            val rootPlan = plan.findRootPlan()
            if (rootPlan.mainPlanFile?.filePath != planId) {
                rootPlan.mainPlanFile?.filePath?.let { rootPlanId ->
                    notifyCostChanged(rootPlanId)
                }
            }
            val totalCost = getTotalCost(planId)
            val totalTokens = getTotalTokens(planId)
            logger.info(
                "Plan $planId: Total cost so far: $${
                    String.format(
                        "%.4f",
                        totalCost
                    )
                }, Total tokens: $totalTokens"
            )
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
        return loadHistoryFromFile(planId)
    }

    fun getTotalCost(planId: String): Double {
        val history = getExecutionHistory(planId)
        return if (history.isEmpty()) 0.0 else history.sumOf { it.sessionCost }
    }

    fun getTotalTokens(planId: String): Int {
        val history = getExecutionHistory(planId)
        return if (history.isEmpty()) 0 else history.sumOf { it.tokensSent + it.tokensReceived }
    }

    private fun updateHistoryFile(plan: AiderPlan, costData: ExecutionCostData, commandData: CommandData) {
        val planFile = plan.mainPlanFile?.filePath ?: return
        val historyFile = File(planFile.replace(".md", HISTORY_FILE_SUFFIX))
        val isNewFile = !historyFile.exists()

        // Create history file if it doesn't exist
        if (isNewFile) {
            createHistoryFile(historyFile, plan)
        }

        // Create the structured CSV-style entry
        val escapedSummary = costData.summary.replace("\n", "\\n").replace(",", "\\,")
        val structuredEntry =
            "$EXECUTION_DATA_PREFIX${costData.timestamp},${costData.model},${costData.tokensSent},${costData.tokensReceived},${costData.messageCost},${costData.sessionCost},$escapedSummary$EXECUTION_DATA_SUFFIX"

        try {
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

                        // Load all entries including the one we just added
                        val allEntries = loadHistoryFromFile(planFile, newContent.toString())
                        
                        // Make sure the new entry is included (in case parsing failed)
                        val combinedEntries = if (allEntries.any { 
                            it.timestamp == costData.timestamp && 
                            it.model == costData.model && 
                            it.sessionCost == costData.sessionCost 
                        }) {
                            allEntries
                        } else {
                            allEntries + costData
                        }
                        
                        // Update the human-readable table with all entries
                        updateHumanReadableTableWithEntries(newContent, plan, combinedEntries)

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

                // Append the entry to the structured data section
                val content = StringBuilder(historyFile.readText())
                content.insert(
                    content.indexOf(EXECUTION_HISTORY_END),
                    structuredEntry + "\n"
                )

                // Update the human-readable table with the new entry
                updateHumanReadableTableWithEntries(content, plan, listOf(costData))

                historyFile.writeText(content.toString())
            }
        } catch (e: Exception) {
            logger.error("Error updating history file", e)
            // Attempt recovery by recreating the file
            try {
                recreateHistoryFile(historyFile, plan, costData)
            } catch (e2: Exception) {
                logger.error("Failed to recover history file", e2)
            }
        }

        // Refresh file in IDE
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(historyFile)
        
        // Notify listeners about the change
        val planId = plan.mainPlanFile?.filePath ?: return
        notifyCostChanged(planId)
        
        // If this was a new file creation, notify for all related plans to ensure UI is updated
        if (isNewFile) {
            // Notify for parent plans
            plan.getAncestors().forEach { ancestor ->
                ancestor.mainPlanFile?.filePath?.let { ancestorId ->
                    notifyCostChanged(ancestorId)
                }
            }
            
            // Notify for child plans
            plan.getAllChildPlans().forEach { child ->
                child.mainPlanFile?.filePath?.let { childId ->
                    notifyCostChanged(childId)
                }
            }
        }
    }


    private fun updateHumanReadableTableWithEntries(content: StringBuilder, plan: AiderPlan, executions: List<ExecutionCostData>) {
        try {
            // Make sure we have a distinct list of executions (no duplicates)
            val distinctExecutions = executions.distinctBy { 
                "${it.timestamp}|${it.model}|${it.tokensSent}|${it.tokensReceived}|${it.sessionCost}" 
            }.sortedByDescending { it.timestamp }
            
            // Remove existing table if present
            val tableStartPattern = "\n## Execution Summary\n"
            val tableStart = content.indexOf(tableStartPattern)
            if (tableStart != -1) {
                // Find the end of the table (next heading or end of file)
                val nextHeading = content.indexOf("\n## ", tableStart + tableStartPattern.length)
                val tableEnd = if (nextHeading != -1) nextHeading else content.length
                content.delete(tableStart, tableEnd)
            }

            // Create a new table with all executions
            val tableBuilder = StringBuilder()
            tableBuilder.append("\n## Execution Summary\n\n")
            
            if (distinctExecutions.isNotEmpty()) {
                tableBuilder.append("| Date | Model | Tokens (Sent/Received) | Cost | Notes |\n")
                tableBuilder.append("| ---- | ----- | --------------------- | ---- | ----- |\n")

                // Add each execution as a row
                distinctExecutions.forEach { execution ->
                    val date = execution.getFormattedTimestamp()
                    // Extract a shorter model name for display
                    val model = try {
                        execution.model
                    } catch (e: Exception) {
                        execution.model.take(10)
                    }

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
                val totalCost = distinctExecutions.sumOf { it.sessionCost }
                val totalTokensSent = distinctExecutions.sumOf { it.tokensSent }
                val totalTokensReceived = distinctExecutions.sumOf { it.tokensReceived }

                tableBuilder.append("\n**Total Cost:** $${String.format("%.4f", totalCost)} | ")
                tableBuilder.append("**Total Tokens:** ${formatTokenCount(totalTokensSent)} sent, ")
                tableBuilder.append("${formatTokenCount(totalTokensReceived)} received | ")
                tableBuilder.append("**Executions:** ${distinctExecutions.size}\n")
            } else {
                tableBuilder.append("*No executions recorded yet*\n")
            }

            // Append the table to the content
            content.append(tableBuilder)
        } catch (e: Exception) {
            logger.error("Error updating human readable table", e)
            // Add a minimal table in case of error
            content.append("\n## Execution Summary\n\n")
            content.append("*Error generating execution summary table. Please check the execution history data.*\n")
        }
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
        
        // Add an empty table structure immediately
        val content = StringBuilder(historyFile.readText())
        updateHumanReadableTableWithEntries(content, plan, emptyList())
        historyFile.writeText(content.toString())
    }

    /**
     * Recreates the history file with proper structure
     */
    private fun recreateHistoryFile(historyFile: File, plan: AiderPlan, costData: ExecutionCostData) {
        try {
            // First, try to extract any existing entries
            val existingEntries = mutableListOf<ExecutionCostData>()
            if (historyFile.exists()) {
                val content = historyFile.readText()

                // Try to parse both old and new format entries
                val oldEntryPattern =
                    Regex("<!-- EXECUTION_ENTRY: ([^|]+)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)(?:\\|([^>]*))? -->")
                val newEntryPattern =
                    Regex("$EXECUTION_DATA_PREFIX([^,]+),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*)(?:,([^>]*))$EXECUTION_DATA_SUFFIX")

                // Parse old format entries
                oldEntryPattern.findAll(content).forEach { matchResult ->
                    try {
                        parseEntryFromRegexMatch(matchResult, existingEntries)
                    } catch (e: Exception) {
                        logger.warn("Failed to parse existing entry (old format): ${e.message}")
                    }
                }

                // Parse new format entries
                newEntryPattern.findAll(content).forEach { matchResult ->
                    try {
                        parseEntryFromRegexMatch(matchResult, existingEntries)
                    } catch (e: Exception) {
                        logger.warn("Failed to parse existing entry (new format): ${e.message}")
                    }
                }
                
                // Also try to parse from the table as a fallback
                if (existingEntries.isEmpty()) {
                    val tablePattern = Regex(
                        "\\| (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) \\| ([^|]+) \\| ([^|]+) / ([^|]+) \\| \\$([^|]+) \\|",
                        RegexOption.MULTILINE
                    )

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

                            existingEntries.add(
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
            }

            // Create a new file
            createHistoryFile(historyFile, plan)

            // Add all existing entries plus the new one
            // Make sure we have no duplicates and the new entry is included
            val allEntries = (existingEntries + costData).distinctBy { 
                "${it.timestamp}|${it.model}|${it.tokensSent}|${it.tokensReceived}|${it.sessionCost}" 
            }

            // Add structured data
            val content = StringBuilder(historyFile.readText())
            
            // Only add the header if it's not already there
            if (!content.toString().contains(EXECUTION_DATA_HEADER)) {
                content.insert(
                    content.indexOf(EXECUTION_HISTORY_END),
                    EXECUTION_DATA_HEADER + "\n"
                )
            }

            // Add entries in reverse chronological order for consistency
            allEntries.sortedByDescending { it.timestamp }.forEach { entry ->
                val escapedSummary = entry.summary.replace("\n", "\\n").replace(",", "\\,")
                val structuredEntry =
                    "$EXECUTION_DATA_PREFIX${entry.timestamp},${entry.model},${entry.tokensSent},${entry.tokensReceived},${entry.messageCost},${entry.sessionCost},$escapedSummary$EXECUTION_DATA_SUFFIX"
                content.insert(
                    content.indexOf(EXECUTION_HISTORY_END),
                    structuredEntry + "\n"
                )
            }

            // Add human-readable table with all entries
            updateHumanReadableTableWithEntries(content, plan, allEntries)

            historyFile.writeText(content.toString())
        } catch (e: Exception) {
            logger.error("Failed to recreate history file", e)
            
            // Last resort: create a minimal valid history file with just the new entry
            try {
                createHistoryFile(historyFile, plan)
                val content = StringBuilder(historyFile.readText())
                
                // Add the new entry
                val escapedSummary = costData.summary.replace("\n", "\\n").replace(",", "\\,")
                val structuredEntry =
                    "$EXECUTION_DATA_PREFIX${costData.timestamp},${costData.model},${costData.tokensSent},${costData.tokensReceived},${costData.messageCost},${costData.sessionCost},$escapedSummary$EXECUTION_DATA_SUFFIX"
                
                content.insert(
                    content.indexOf(EXECUTION_HISTORY_END),
                    EXECUTION_DATA_HEADER + "\n" + structuredEntry + "\n"
                )
                
                // Add a simple table with just the new entry
                updateHumanReadableTableWithEntries(content, plan, listOf(costData))
                
                historyFile.writeText(content.toString())
            } catch (e2: Exception) {
                logger.error("Critical failure recreating history file", e2)
            }
        }
    }

    /**
     * Helper method to parse execution data from regex match
     */
    private fun parseEntryFromRegexMatch(
        matchResult: MatchResult,
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
            val tokensSent = parseTokenCount(tokensSentStr)
            val tokensReceived = parseTokenCount(tokensReceivedStr)
            val messageCost = parseCostValue(messageCostStr)
            val sessionCost = parseCostValue(sessionCostStr)

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

    private fun parseCostValue(costStr: String): Double =
        costStr
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0

    private fun loadHistoryFromFile(planId: String, contentOverride: String? = null): List<ExecutionCostData> {
        try {
            val historyFile = File(planId.replace(".md", HISTORY_FILE_SUFFIX))
            if (!historyFile.exists() && contentOverride == null) {
                return emptyList()
            }

            val content = contentOverride ?: historyFile.readText()
            val executionEntries = mutableListOf<ExecutionCostData>()

            // Try to parse both old and new format entries
            val oldEntryPattern =
                Regex("<!-- EXECUTION_ENTRY: ([^|]+)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)(?:\\|([^>]*))? -->")
            val newEntryPattern =
                Regex("$EXECUTION_DATA_PREFIX([^,]+),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*)(?:,([^>]*))$EXECUTION_DATA_SUFFIX")

            // Parse old format entries
            oldEntryPattern.findAll(content).forEach { matchResult ->
                try {
                    parseEntryFromRegexMatch(matchResult, executionEntries)
                } catch (e: Exception) {
                    logger.warn("Failed to parse execution entry (old format)", e)
                }
            }

            // Parse new format entries
            newEntryPattern.findAll(content).forEach { matchResult ->
                try {
                    parseEntryFromRegexMatch(matchResult, executionEntries)
                } catch (e: Exception) {
                    logger.warn("Failed to parse execution entry (new format)", e)
                }
            }

            // If no structured entries found, try to parse from the table format as fallback
            if (executionEntries.isEmpty()) {
                val tablePattern = Regex(
                    "\\| (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) \\| ([^|]+) \\| ([^|]+) / ([^|]+) \\| \\$([^|]+) \\|",
                    RegexOption.MULTILINE
                )

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

            // We no longer cache entries as we always read from file
            return executionEntries
        } catch (e: Exception) {
            logger.warn("Failed to load history from file for plan: $planId", e)
            return emptyList()
        }
    }

    private fun parseTokenCount(tokenStr: String): Int {
        return when {
            tokenStr.endsWith("k") -> {
                val value = tokenStr.substring(0, tokenStr.length - 1).replace(",", ".").toDoubleOrNull() ?: 0.0
                (value * 1000).toInt()
            }

            else -> tokenStr.replace(",", ".").toIntOrNull() ?: 0
        }
    }
}
