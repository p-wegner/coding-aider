package de.andrena.codingaider.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
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
                .directory(java.io.File(workingDir))
                .redirectErrorStream(true)

            process = processBuilder.start()
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))

            // Wait for the userPromptMarker before marking the process as running
            var line: String? = null
            while (reader?.readLine()?.also { line = it } != null) {
                println(line)
                if (line?.startsWith(userPromptMarker) == true || line?.trim() == "") {
                    isRunning.set(true)
                    reader?.mark(20000)
                    break
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

    fun sendCommand(command: String): String {
        if (!isRunning.get()) {
            throw IllegalStateException("Aider sidecar process not running")
        }

        return try {
            reader?.reset()
            writer?.write("$command\n")
            writer?.flush()

            // Read response until we get the prompt marker
            val response = StringBuilder()
            var line: String? = null
            var promptFound = false
            while (reader?.readLine()?.also { line = it } != null) {
                if (line?.startsWith(userPromptMarker) == true && false) {
                    promptFound = true
                    break
                }
                response.append(line).append("\n")
            }

            if (!promptFound) {
                logger.warn("No prompt marker found in Aider sidecar process output")
            }

            response.toString().trim()
        } catch (e: Exception) {
            logger.error("Error sending command to Aider sidecar process", e)
            throw e
        }
    }

    override fun dispose() {
        try {
            writer?.close()
            reader?.close()
            process?.destroy()
            isRunning.set(false)
            logger.info("Disposed Aider sidecar process")
            // poll for process to exit for 5 seconds
            // if it doesn't exit, forcefully kill it
            var attempts = 0
            while (process?.isAlive == true && attempts < 50) {
                Thread.sleep(100)
                attempts++
            }
            process?.destroyForcibly()
            Thread.sleep(10)
        } catch (e: Exception) {
            logger.error("Error disposing Aider sidecar process", e)
        }
    }
}
