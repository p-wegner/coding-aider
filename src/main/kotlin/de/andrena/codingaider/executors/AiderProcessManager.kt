package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.io.*
import java.util.concurrent.TimeUnit

class AiderProcessManager(
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) {
    private val logger = Logger.getInstance(AiderProcessManager::class.java)
    private val settings = AiderSettings.getInstance(project)
    private var process: Process? = null
    private var inputWriter: BufferedWriter? = null
    private var outputReader: BufferedReader? = null
    private val dockerManager = DockerContainerManager()

    fun startAiderProcess(commandData: CommandData) {
        val executionStrategy = if (settings.useDockerAider) {
            DockerAiderExecutionStrategy(dockerManager, apiKeyChecker, settings)
        } else {
            NativeAiderExecutionStrategy(apiKeyChecker, settings)
        }

        val commandArgs = executionStrategy.buildCommand(commandData)
        logger.info("Starting Aider process: ${commandArgs.joinToString(" ")}")

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
    }

    fun sendCommand(command: String): String {
        inputWriter?.write(command)
        inputWriter?.newLine()
        inputWriter?.flush()

        return readOutput()
    }

    private fun readOutput(): String {
        val output = StringBuilder()
        var line: String?
        while (outputReader?.readLine().also { line = it } != null) {
            if (line == "> ") break
            output.append(line).append("\n")
        }
        return output.toString().trim()
    }

    fun stopAiderProcess() {
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
    }
}
