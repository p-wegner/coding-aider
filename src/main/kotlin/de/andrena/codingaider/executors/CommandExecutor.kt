package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.io.File
import java.util.concurrent.TimeUnit

class CommandExecutor(
    project: Project,
    private val commandData: CommandData,
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) :
    CommandSubject by GenericCommandSubject() {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private val settings = AiderSettings.getInstance(project)
    private val commandLogger = CommandLogger(settings, commandData)
    private var process: Process? = null
    private var isAborted = false
    private val useDockerAider: Boolean
        get() = commandData.useDockerAider ?: settings.useDockerAider
    private val dockerManager = DockerContainerManager()
    private val executionStrategy: AiderExecutionStrategy by lazy {
        if (useDockerAider) DockerAiderExecutionStrategy(
            dockerManager,
            apiKeyChecker
        ) else NativeAiderExecutionStrategy(apiKeyChecker)
    }

    fun executeCommand(): String {
        val commandArgs = executionStrategy.buildCommand(commandData)
        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")
        notifyObservers {
            it.onCommandStart(
                "Starting Aider command...\n${
                    commandLogger.getCommandString(
                        false,
                        if (useDockerAider) dockerManager else null
                    )
                }"
            )
        }

        val processBuilder = ProcessBuilder(commandArgs)
            .directory(File(commandData.projectPath))
            .apply {
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
            }

        executionStrategy.prepareEnvironment(processBuilder, commandData)

        process = processBuilder.start()

        val output = StringBuilder()
        try {
            pollProcessAndReadOutput(process!!, output)
            return handleProcessCompletion(process!!, output)
        } finally {
            executionStrategy.cleanupAfterExecution()
        }
    }

    fun abortCommand() {
        isAborted = true
        if (useDockerAider) {
            dockerManager.stopDockerContainer()
        } else {
            process?.destroyForcibly()
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
