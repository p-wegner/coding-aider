package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.diagnostic.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.scheduler.Schedulers
import java.io.BufferedReader
import java.io.BufferedWriter
import java.util.concurrent.TimeUnit

class RobustAiderOutputParser(
    private val verbose: Boolean,
    private val logger: Logger,
    private val reader: BufferedReader?,
    private val writer: BufferedWriter?
) : AiderOutputParser {

    companion object {
        private const val READ_TIMEOUT_MS = 500L
        private const val MAX_SILENT_READS = 10
        private const val PROMPT_MARKER = ">"
        private const val SUMMARY_START = "<aider-summary>"
        private const val SUMMARY_END = "</aider-summary>"
    }

    override fun writeCommandAndReadResults(command: String, sink: FluxSink<String>) {
        try {
            writer?.write("$command\n")
            writer?.flush()

            var silentReads = 0
            var lastActivityTime = System.currentTimeMillis()
            var inSummaryBlock = false
            val currentSummary = StringBuilder()
            var buffer = StringBuilder()

            while (true) {
                val line = reader?.readLine()
                
                when {
                    line == null -> {
                        // End of stream
                        if (inSummaryBlock) {
                            completeSummary(sink, currentSummary)
                        } else if (buffer.isNotEmpty()) {
                            sink.next(buffer.toString())
                        }
                        sink.complete()
                        return
                    }
                    
                    line.trim() == SUMMARY_START -> {
                        inSummaryBlock = true
                        currentSummary.clear()
                        currentSummary.append(line).append("\n")
                    }
                    
                    line.trim() == SUMMARY_END -> {
                        currentSummary.append(line).append("\n")
                        sink.next(currentSummary.toString())
                        inSummaryBlock = false
                        currentSummary.clear()
                    }
                    
                    inSummaryBlock -> {
                        currentSummary.append(line).append("\n")
                    }
                    
                    line.trim() == PROMPT_MARKER -> {
                        // Found prompt marker - complete output
                        if (buffer.isNotEmpty()) {
                            sink.next(buffer.toString())
                            buffer.clear()
                        }
                        sink.complete()
                        return
                    }
                    
                    else -> {
                        // Regular output line
                        buffer.append(line).append("\n")
                        lastActivityTime = System.currentTimeMillis()
                        silentReads = 0
                    }
                }

                // Handle silent periods
                if (line == null || line.isBlank()) {
                    silentReads++
                    if (silentReads >= MAX_SILENT_READS) {
                        // No activity for a while - assume output is complete
                        if (buffer.isNotEmpty()) {
                            sink.next(buffer.toString())
                            buffer.clear()
                        }
                        sink.complete()
                        return
                    }
                }

                // Handle timeout
                if (System.currentTimeMillis() - lastActivityTime > READ_TIMEOUT_MS) {
                    if (buffer.isNotEmpty()) {
                        sink.next(buffer.toString())
                        buffer.clear()
                    }
                    sink.complete()
                    return
                }

                // Small sleep to prevent CPU spinning
                TimeUnit.MILLISECONDS.sleep(10)
            }
        } catch (e: Exception) {
            logger.error("Error in RobustAiderOutputParser", e)
            sink.error(OutputParsingException("Error parsing Aider output: ${e.message}", e))
        }
    }

    private fun completeSummary(sink: FluxSink<String>, summary: StringBuilder) {
        if (summary.isNotEmpty()) {
            summary.append("</aider-summary>\n")
            sink.next(summary.toString())
        }
    }
}
fun streamProcessOutputAsFlux(process: Process): Flux<String> {
    return Flux.create<String> { sink ->
        // Use a dedicated thread to read blocking I/O
        val readerThread = Thread {
            try {
                process.inputStream.bufferedReader().use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        sink.next(line)  // emit each line
                    }
                    // If we exit the loop normally, we've hit EOF
                    sink.complete()
                }
            } catch (e: Exception) {
                sink.error(e)
            }
        }

        readerThread.start()

        // If the subscriber cancels, interrupt the reading thread
        sink.onCancel {
            readerThread.interrupt()
        }
    }
        // Move blocking reads off the main thread to avoid blocking the reactive pipeline
        .subscribeOn(Schedulers.boundedElastic())
}
