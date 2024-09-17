package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.*
import java.util.concurrent.TimeUnit

class AiderProcessManager(
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) : CommandSubject by GenericCommandSubject() {
    private val logger = Logger.getInstance(AiderProcessManager::class.java)
    private val settings = AiderSettings.getInstance(project)
    private var process: Process? = null
    private var inputWriter: BufferedWriter? = null
    private var outputReader: BufferedReader? = null
    private val dockerManager = DockerContainerManager()
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Default)

    fun startAiderProcess(commandData: CommandData) {
        val executionStrategy = if (settings.useDockerAider) {
            DockerAiderExecutionStrategy(dockerManager, apiKeyChecker, settings)
        } else {
            NativeAiderExecutionStrategy(apiKeyChecker, settings)
        }

        val commandArgs = executionStrategy.buildCommand(commandData)
        logger.info("Starting Aider process: ${commandArgs.joinToString(" ")}")

        notifyObservers { it.onCommandStart("Starting Aider process...") }

        val processBuilder = ProcessBuilder(commandArgs)
            .directory(File(commandData.projectPath))
            .apply {
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
            }

        executionStrategy.prepareEnvironment(processBuilder, commandData)

        process = processBuilder.start()
        inputWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream))
        outputReader = BufferedReader(InputStreamReader(process!!.inputStream))

        isRunning = true
        startOutputReading()
    }

    fun sendCommand(command: String): String {
        inputWriter?.write(command)
        inputWriter?.newLine()
        inputWriter?.flush()

        val output = StringBuilder()
        var line: String? = null
        while (isRunning && outputReader?.readLine().also { line = it } != null) {
            if (line == "> ") break
            output.append(line).append("\n")
            notifyObservers { it.onCommandProgress(output.toString(), 0) }
        }
        return output.toString().trim()
    }

    private fun startOutputReading() {
        scope.launch {
            val startTime = System.currentTimeMillis()
            while (isRunning) {
                val line = outputReader?.readLine() ?: break
                val runningTime = (System.currentTimeMillis() - startTime) / 1000
                notifyObservers { it.onCommandProgress(line, runningTime) }
            }
        }
    }

    fun stopAiderProcess() {
        isRunning = false
        inputWriter?.close()
        outputReader?.close()
        process?.let {
            if (it.isAlive) {
                it.destroy()
                if (!it.waitFor(5, TimeUnit.SECONDS)) {
                    it.destroyForcibly()
                }
            }
        }
        if (settings.useDockerAider) {
            dockerManager.stopDockerContainer()
        }
        notifyObservers { it.onCommandComplete("Aider process stopped", 0) }
        scope.cancel()
    }
}
