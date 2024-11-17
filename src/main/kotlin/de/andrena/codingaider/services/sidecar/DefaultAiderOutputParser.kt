package de.andrena.codingaider.services.sidecar

import com.intellij.openapi.diagnostic.Logger
import reactor.core.publisher.FluxSink
import java.io.BufferedReader
import java.io.BufferedWriter

class DefaultAiderOutputParser(
    private val verbose: Boolean,
    private val logger: Logger,
    private val reader: BufferedReader?,
    private val writer: BufferedWriter?
) : AiderOutputParser {
    val terminalPromptPrefix = listOf("Tokens: ", "Dropping all files from the chat session")
    private val commandPrompt = "> "
    private fun String.isPromptLine() = this == commandPrompt
    override fun writeCommandAndReadResults(command: String, sink: FluxSink<String>) {
        try {
            writer?.write("$command\n")
            writer?.flush()

            var commandPromptCount = 0
            var terminalPromptPrefixHitCount = 0
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                if (verbose) logger.info(line)
                if (!line!!.isPromptLine()) {
                    sink.next(line!!)
                }

                if (line == commandPrompt) commandPromptCount++
                if (terminalPromptPrefix.any { line!!.startsWith(it) }) terminalPromptPrefixHitCount++
                if (commandPromptCount > 0 && (
                            terminalPromptPrefixHitCount > 0
                                    || command == "/clear"
                                    || command.startsWith("/add")
                                    || command.startsWith("/read-only")
                            )
                ) {
                    sink.complete()
                    return
                }
            }
            sink.error(IllegalStateException("Process terminated while waiting for response"))
        } catch (e: Exception) {
            sink.error(e)
        }
    }

}