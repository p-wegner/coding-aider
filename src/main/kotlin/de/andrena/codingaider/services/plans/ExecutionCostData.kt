package de.andrena.codingaider.services.plans

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

    companion object {
        fun fromCommandOutput(output: String): ExecutionCostData {
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

            // Cost: $0.0085 message, $0.0085 session.
            val costRegex = listOf(
                Regex("Cost:\\s*\\$(\\d+(?:[\\.,]\\d+)?)\\s*message,\\s*\\$(\\d+(?:[\\.,]\\d+)?)\\s*session"),
                Regex("\\$(\\d+(?:[\\.,]\\d+)?)\\s*message,\\s*\\$(\\d+(?:[\\.,]\\d+)?)\\s*session")
            )

            for (regex in costRegex) {
                regex.findAll(output).lastOrNull()?.let {
                    messageCost = parseCostValue(it.groupValues[1])
                    sessionCost = parseCostValue(it.groupValues[2])
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

        private fun parseCostValue(costStr: String): Double {
            // Handle international number formats by replacing comma with dot
            val normalizedStr = costStr.replace(",", ".")
            return normalizedStr.toDoubleOrNull() ?: 0.0
        }
    }
}