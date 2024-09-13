package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.ApiKeyManager
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class CommandExecutor(private val project: Project, private val commandData: CommandData) :
    CommandSubject by GenericCommandSubject() {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private val settings = AiderSettings.getInstance(project)
    private val commandLogger = CommandLogger(settings, commandData)
    private var process: Process? = null
    private var isAborted = false
    private var dockerContainerId: String? = null
    private val useDockerAider: Boolean
        get() = commandData.useDockerAider ?: settings.useDockerAider
    private val cidFilePath = "/tmp/aider_container_id"

    fun executeCommand(): String {
        val commandArgs = AiderCommandBuilder.buildAiderCommand(
            commandData,
            false,
            useDockerAider
        )
        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")
        notifyObservers { it.onCommandStart("Starting Aider command...\n${commandLogger.getCommandString(false)}") }

        val processBuilder = ProcessBuilder(commandArgs)
            .directory(File(commandData.projectPath))
            .apply {
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
            }

        if (useDockerAider) {
            // Use the default Docker host, which should work across platforms
            processBuilder.environment().remove("DOCKER_HOST")
        } else {
            // Set API key environment variables when not using Docker
            ApiKeyChecker.getAllApiKeyNames().forEach { keyName ->
                val apiKey = ApiKeyManager.getApiKey(keyName)
                if (!apiKey.isNullOrBlank()) {
                    processBuilder.environment()[keyName] = apiKey
                }
            }
        }

        process = processBuilder.start()

        if (useDockerAider) {
            dockerContainerId = getDockerContainerId()
        }

        val output = StringBuilder()
        pollProcessAndReadOutput(process!!, output)
        return handleProcessCompletion(process!!, output)
    }

    fun abortCommand() {
        isAborted = true
        if (useDockerAider) {
            stopDockerContainer()
        } else {
            process?.destroyForcibly()
        }
    }

    private fun getDockerContainerId(): String? {
        // Wait for the cidfile to be created
        var attempts = 0
        while (attempts < 10) {
            if (Files.exists(Paths.get(cidFilePath))) {
                return Files.readString(Paths.get(cidFilePath)).trim()
            }
            Thread.sleep(500)
            attempts++
        }
        logger.warn("Failed to read Docker container ID from cidfile")
        return null
    }

    private fun stopDockerContainer() {
        dockerContainerId?.let { containerId ->
            try {
                val processBuilder = ProcessBuilder("docker", "stop", "--time", "0", containerId)
                val stopProcess = processBuilder.start()
                if (!stopProcess.waitFor(5, TimeUnit.SECONDS)) {
                    stopProcess.destroyForcibly()
                    logger.warn("Docker stop command timed out")
                }
                // Clean up the cidfile
                Files.deleteIfExists(Paths.get(cidFilePath))
            } catch (e: Exception) {
                logger.error("Failed to stop Docker container", e)
            }
        }
    }

    private fun handleProcessCompletion(process: Process, output: StringBuilder): String {
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroy()
            val errorMessage = commandLogger.prependCommandToOutput("$output\nAider command timed out after 5 minutes")
            notifyObservers { it.onCommandError(errorMessage) }
            return errorMessage
        } else {
            val exitCode = process.exitValue()
            val status = if (exitCode == 0) "executed successfully" else "failed with exit code $exitCode"
            val finalOutput = commandLogger.prependCommandToOutput("$output\nAider command $status")
            notifyObservers { it.onCommandComplete(finalOutput, exitCode) }
            return finalOutput
        }
    }

    private fun pollProcessAndReadOutput(process: Process, output: StringBuilder) {
        val startTime = System.currentTimeMillis()
        process.inputStream.bufferedReader().use { reader ->
            while (!isAborted) {
                val line = reader.readLine() ?: break
                output.append(line).append("\n")
                val runningTime = (System.currentTimeMillis() - startTime) / 1000
                notifyObservers {
                    it.onCommandProgress(
                        commandLogger.prependCommandToOutput(output.toString()),
                        runningTime
                    )
                }
                if (!process.isAlive || runningTime > 300) break
                Thread.sleep(10)
            }
        }
    }
}
