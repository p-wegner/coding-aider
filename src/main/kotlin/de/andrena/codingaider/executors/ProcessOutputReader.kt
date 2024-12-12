package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import de.andrena.codingaider.executors.api.CommandObserver
import java.io.IOException
import java.io.InputStream

class ProcessOutputReader(
    private val process: Process,
    private val output: StringBuilder,
    private val commandLogger: CommandLogger,
    private val startTime: Long,
    private val isAbortedCallback: () -> Boolean,
    private val notifyObservers: ((CommandObserver) -> Unit) -> Unit
) {
    private val logger = Logger.getInstance(ProcessOutputReader::class.java)

    fun start() {
        try {
            val stdoutThread = startStreamReader(process.inputStream, "stdout")
            val stderrThread = startStreamReader(process.errorStream, "stderr")

            // Wait for both streams to complete
            stdoutThread.join()
            stderrThread.join()
        } catch (e: InterruptedException) {
            logger.info("Process output reading interrupted", e)
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            logger.error("Error reading process output", e)
            output.append("Error reading process output: ${e.message}\n")
        }
    }

    private fun startStreamReader(inputStream: InputStream, streamName: String): Thread {
        return Thread({
            try {
                inputStream.bufferedReader().use { reader ->
                    val buffer = CharArray(8192)
                    while (!isAbortedCallback() && process.isAlive) {
                        val count = reader.read(buffer)
                        if (count == -1) break

                        synchronized(output) {
                            output.append(buffer, 0, count)
                            notifyProgress()
                        }
                    }
                }
            } catch (e: IOException) {
                if (!isAbortedCallback()) {
                    logger.error("Error reading $streamName", e)
                }
            }
        }, "ProcessReader-$streamName").apply { start() }
    }

    private fun notifyProgress() {
        val runningTime = secondsSince(startTime)
        notifyObservers {
            it.onCommandProgress(
                commandLogger.prependCommandToOutput(output.toString()),
                runningTime
            )
        }
    }
}