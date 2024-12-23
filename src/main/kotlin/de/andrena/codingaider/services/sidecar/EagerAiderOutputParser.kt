package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.diagnostic.Logger
import reactor.core.publisher.FluxSink
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.concurrent.TimeUnit

class EagerAiderOutputParser(
    private val verbose: Boolean,
    private val logger: Logger,
    private val reader: BufferedReader?,
    private val writer: BufferedWriter?
) : AiderOutputParser {
    private val readTimeout = 250L // Reduced timeout for better responsiveness
    private val maxEmptyReads = 20 // Increased to handle slower responses
    private val commandPrompt = "> "
    private val summaryStartMarker = "<aider-summary>"
    private val summaryEndMarker = "</aider-summary>"
    private var inSummaryBlock = false
    private val currentSummary = StringBuilder()
    private var hasSeenValidResponse = false
    private var lastValidResponseTime = 0L
    
    private fun String.isPromptLine() = this == commandPrompt
    
    private fun resetState() {
        hasSeenValidResponse = false
        lastValidResponseTime = 0L
        currentSummary.clear()
        inSummaryBlock = false
    }

    override fun writeCommandAndReadResults(command: String, sink: FluxSink<String>) {
        try {
            resetState()
            writer?.write("$command\n")
            writer?.flush()

            var lastReadTime = System.currentTimeMillis()
            var emptyReads = 0
            var lastChar: Int? = null

            while (true) {
                if (reader?.ready() == true) {
                    emptyReads = 0
                    reader.mark(1000)
                    val char = reader.read()
                    
                    when {
                        char == -1 -> {
                            handleEndOfStream(sink)
                            return
                        }
                        char == 62 && lastChar == 10 -> { // "\n>" sequence
                            handlePrompt(sink)
                            return
                        }
                        else -> {
                            reader.reset()
                            val line = reader.readLine()
                            handleLine(line, sink)
                            lastReadTime = System.currentTimeMillis()
                            if (!line.isPromptLine()) {
                                hasSeenValidResponse = true
                                lastValidResponseTime = lastReadTime
                            }
                            lastChar = 10 // Line feed after readLine()
                        }
                    }
                } else {
                    if (handleEmptyRead(emptyReads++, lastReadTime, sink)) return
                    TimeUnit.MILLISECONDS.sleep(10) // Further reduced sleep time
                }
            }
        } catch (e: Exception) {
            handleError(e, sink)
        }
    }

    private fun handleEndOfStream(sink: FluxSink<String>) {
        if (inSummaryBlock) {
            completeSummaryBlock(sink)
        }
        // Only complete if we've seen a valid response
        if (hasSeenValidResponse) {
            sink.complete()
        } else {
            sink.error(OutputParsingException("Stream ended without valid response"))
        }
    }

    private fun handlePrompt(sink: FluxSink<String>) {
        if (inSummaryBlock) {
            completeSummaryBlock(sink)
        }
        sink.complete()
    }

    private fun handleLine(line: String, sink: FluxSink<String>) {
        if (verbose) logger.info(line)
        
        when {
            line.trim() == summaryStartMarker -> {
                inSummaryBlock = true
                currentSummary.clear()
                currentSummary.append(line).append("\n")
            }
            line.trim() == summaryEndMarker -> {
                currentSummary.append(line).append("\n")
                sink.next(currentSummary.toString())
                inSummaryBlock = false
                currentSummary.clear()
            }
            inSummaryBlock -> {
                currentSummary.append(line).append("\n")
            }
            !line.isPromptLine() -> {
                sink.next(line)
            }
        }
    }

    private fun handleEmptyRead(emptyReads: Int, lastReadTime: Long, sink: FluxSink<String>): Boolean {
        val timeSinceLastRead = System.currentTimeMillis() - lastReadTime
        if (timeSinceLastRead > readTimeout || emptyReads > maxEmptyReads) {
            if (inSummaryBlock) {
                completeSummaryBlock(sink)
            }
            sink.complete()
            return true
        }
        return false
    }

    private fun completeSummaryBlock(sink: FluxSink<String>) {
        if (currentSummary.isNotEmpty()) {
            currentSummary.append("</aider-summary>\n")
            sink.next(currentSummary.toString())
            currentSummary.clear()
        }
        inSummaryBlock = false
    }

    private fun handleError(e: Exception, sink: FluxSink<String>) {
        logger.error("Error in EagerAiderOutputParser", e)
        if (inSummaryBlock) {
            completeSummaryBlock(sink)
        }
        val wrappedException = when (e) {
            is IllegalStateException -> OutputParsingException("Parser state error: ${e.message}", e)
            is SecurityException -> OutputParsingException("Security error during parsing: ${e.message}", e)
            is OutputParsingException -> e
            else -> OutputParsingException("Unexpected error during parsing: ${e.message}", e)
        }
        sink.error(wrappedException)
    }
}
