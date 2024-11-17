package de.andrena.codingaider.services

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
) :AiderOutputParser{
    private val readTimeout = 300L // milliseconds
    private val commandPrompt = "> "
    private fun String.isPromptLine() = this == commandPrompt

    override fun writeCommandAndReadResults(command: String, sink: FluxSink<String>) {
        try {
            writer?.write("$command\n")
            writer?.flush()

            var lastReadTime = System.currentTimeMillis()
            
            while (true) {
                if (reader?.ready() == true) {
                    reader.mark(1000) // Mark the current position
                    val char = reader.read()
                    if (char == -1) {
                        // End of stream
                        sink.complete()
                        return
                    }
                    reader.reset() // Go back to marked position
                    
                    val line = reader.readLine()
                    if (verbose) logger.info(line)
                    if (!line.isPromptLine()) {
                        sink.next(line)
                    }
                    lastReadTime = System.currentTimeMillis()
                } else {
                    // Check if we've exceeded the timeout
                    if (System.currentTimeMillis() - lastReadTime > readTimeout) {
                        sink.complete()
                        return
                    }
                    TimeUnit.MILLISECONDS.sleep(50) // Small sleep to prevent busy waiting
                }
            }
        } catch (e: Exception) {
            sink.error(e)
        }
    }
}
