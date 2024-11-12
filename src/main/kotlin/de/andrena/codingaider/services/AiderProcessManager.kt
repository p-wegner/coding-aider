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

    fun startProcess(command: List<String>, workingDir: String): Boolean {
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
            isRunning.set(true)
            
            logger.info("Started Aider sidecar process")
            true
        } catch (e: Exception) {
            logger.error("Failed to start Aider sidecar process", e)
            false
        }
    }

    fun sendCommand(command: String): String {
        if (!isRunning.get()) {
            throw IllegalStateException("Aider sidecar process not running")
        }

        return try {
            writer?.write("$command\n")
            writer?.flush()
            
            // Read response until we get the prompt marker
            val response = StringBuilder()
            var line: String?
            while (reader?.readLine()?.also { line = it } != null) {
                if (line?.contains("🤖>") == true) break
                response.append(line).append("\n")
            }
            response.toString()
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
        } catch (e: Exception) {
            logger.error("Error disposing Aider sidecar process", e)
        }
    }
}
