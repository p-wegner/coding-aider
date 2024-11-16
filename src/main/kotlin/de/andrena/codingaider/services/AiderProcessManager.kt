package de.andrena.codingaider.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class AiderProcessManager(private val project: Project) : Disposable {
    private val logger = Logger.getInstance(AiderProcessManager::class.java)
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val outputChannel = Channel<String>(Channel.BUFFERED)
    private val isRunning = AtomicBoolean(false)

    fun startProcess(
        command: List<String>,
        workingDir: String,
        maxIdleTime: Int = 0,
        autoRestart: Boolean = false,
        verbose: Boolean = false
    ): Boolean {
        if (isRunning.get()) {
            logger.info("Aider sidecar process already running")
            return true
        }

        return try {
            val processBuilder = ProcessBuilder(command)
                .apply { environment().putIfAbsent("PYTHONIOENCODING", "utf-8") }
                .directory(java.io.File(workingDir))
                .redirectErrorStream(true)

            process = processBuilder.start()
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            // Start coroutine to handle process output
            scope.launch {
                reader?.useLines { lines ->
                    lines.forEach { line ->
                        outputChannel.send(line)
                        println(line)
                        if (line.startsWith(userPromptMarker) || line.trim().isEmpty()) {
                            isRunning.set(true)
                        }
                    }
                }
            }

            if (verbose) {
                logger.info("Started Aider sidecar process with command: ${command.joinToString(" ")}")
                logger.info("Working directory: $workingDir")
                logger.info("Max idle time: $maxIdleTime")
                logger.info("Auto restart: $autoRestart")
            }

            logger.info("Started Aider sidecar process")
            true
        } catch (e: Exception) {
            logger.error("Failed to start Aider sidecar process", e)
            false
        }
    }

    private val userPromptMarker = "> "

    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isRunning.get()) {
            throw IllegalStateException("Aider sidecar process not running")
        }

        try {
            writer?.write("$command\n")
            writer?.flush()

            val response = StringBuilder()
            while (true) {
                val line = outputChannel.receive()
                if (line.startsWith(userPromptMarker)) {
                    break
                }
                response.append(line).append("\n")
            }
            response.toString().trim()
        } catch (e: Exception) {
            logger.error("Error sending command to Aider sidecar process", e)
            throw e
        }
    }

    override fun dispose() {
        runBlocking {
            try {
                scope.cancel()
                writer?.close()
                reader?.close()
                process?.destroy()
                isRunning.set(false)
                logger.info("Disposed Aider sidecar process")
                
                withTimeout(5000) {
                    while (process?.isAlive == true) {
                        delay(100)
                    }
                }
                
                process?.destroyForcibly()
                delay(10)
            } catch (e: Exception) {
                logger.error("Error disposing Aider sidecar process", e)
            }
        }
    }

    fun isReadyForCommand(): Boolean {
        if (process?.isAlive != true) return false
        // TODO: Implement actual readiness check
        return true

    }
}
