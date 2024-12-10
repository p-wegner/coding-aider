package de.andrena.codingaider.executors

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.executors.api.AiderProcessInteractor
import de.andrena.codingaider.executors.api.CommandObserver
import de.andrena.codingaider.executors.api.CommandSubject
import de.andrena.codingaider.executors.api.DefaultAiderProcessInteractor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileExtractorService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.sidecar.AiderProcessManager
import de.andrena.codingaider.services.sidecar.SidecarProcessInitializer
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class CommandExecutor(
    private val commandData: CommandData,
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) :
    CommandSubject by GenericCommandSubject() {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private val settings = getInstance()
    private val commandLogger = CommandLogger(project, settings, commandData)
    private var process: Process? = null
    private var isAborted = false
    private val useDockerAider: Boolean
        get() = commandData.options.useDockerAider ?: settings.useDockerAider
    private val dockerManager = DockerContainerManager()
    private val executionStrategy: AiderExecutionStrategy by lazy {
        if (useDockerAider) DockerAiderExecutionStrategy(
            project,
            dockerManager,
            apiKeyChecker,
            settings
        ) else NativeAiderExecutionStrategy(project, apiKeyChecker, settings)
    }
    private val processInteractor: AiderProcessInteractor by lazy { DefaultAiderProcessInteractor(project) }

    private val aiderPlanService = project.service<AiderPlanService>()

    fun executeCommand(): String {
        aiderPlanService.createPlanFolderIfNeeded(commandData)
        val updatedCommandData = extractFilesIfNeeded()
        if (commandData.sidecarMode) {
            return startSideCarAndExecuteCommand(updatedCommandData)
        }

        return executeCommandInNewProcess(updatedCommandData)
    }


    private fun executeCommandInNewProcess(updatedCommandData: CommandData): String {
        val commandArgs = executionStrategy.buildCommand(updatedCommandData)
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
            .apply {
                if (commandData.projectPath.isNotEmpty()) directory(File(commandData.projectPath))
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

    private fun executeSidecarCommand(commandData: CommandData): String {
        val commandString = buildSidecarCommandString(commandData)
        logger.info("Executing Sidecar Aider command: $commandString")

        notifyObservers {
            it.onCommandStart(
                "Starting Sidecar Aider command...\n${
                    commandLogger.getCommandString(
                        false,
                        null
                    )
                }"
            )
        }

        changeContextFiles(commandData)
        val output = try {
            val startTime = System.currentTimeMillis()
            val output = StringBuilder()
            val response = processInteractor.sendCommandAsync(commandString)
                .doOnNext { message ->
                    output.append(message).append("\n")
                    notifyObservers {
                        it.onCommandProgress(
                            commandLogger.prependCommandToOutput(output.toString()),
                            secondsSince(startTime)
                        )
                    }
                }
                .collectList().block()?.joinToString("\n") ?: ""
            notifyObservers { it.onCommandComplete(response, 0) }
            response
        } catch (e: Exception) {
            val errorMessage = "Sidecar command failed: ${e.message}"
            logger.error(errorMessage, e)
            notifyObservers { it.onCommandError(errorMessage) }
            errorMessage
        }

        return commandLogger.prependCommandToOutput(output)
    }

    private fun extractFilesIfNeeded(): CommandData {
        val fileExtractorService = FileExtractorService.getInstance()
        val extractedFiles = fileExtractorService.extractFilesIfNeeded(commandData.files)
        val updatedCommandData = commandData.copy(files = extractedFiles)
        return updatedCommandData
    }

    private fun startSideCarAndExecuteCommand(updatedCommandData: CommandData): String {
        startSideCarWithTimeout()
        return executeSidecarCommand(updatedCommandData)
    }

    private fun startSideCarWithTimeout() {
        project.service<SidecarProcessInitializer>().initializeSidecarProcess()
        val startTime = System.currentTimeMillis()
        while (!project.service<AiderProcessManager>().isReadyForCommand()) {
            Thread.sleep(100)
            if (System.currentTimeMillis() - startTime > 10000) {
                throw IllegalStateException("Sidecar process failed to start")
            }
        }
    }

    private fun changeContextFiles(commandData: CommandData) {
        processInteractor.sendCommandSync("/drop")
        processInteractor.sendCommandSync("/clear")
        // TODO: Termination of add and read-only commands is not detected correctly
        commandData.files.filter { !it.isReadOnly }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { it.filePath }
            ?.let { files -> processInteractor.sendCommandSync("/add $files") }
        commandData.files.filter { it.isReadOnly }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { it.filePath }
            ?.let { files -> processInteractor.sendCommandSync("/read-only $files") }

    }

    private fun buildSidecarCommandString(commandData: CommandData): String {
        return when (commandData.aiderMode) {
            AiderMode.NORMAL -> commandData.message
            AiderMode.STRUCTURED -> project.service<AiderPlanService>().createAiderPlanSystemPrompt(commandData)
            AiderMode.ARCHITECT -> "/architect ${commandData.message}"
            else -> ""
        }
    }

    fun abortCommand() {
        isAborted = true
        if (commandData.sidecarMode) {
            project.service<AiderProcessManager>().interruptCurrentCommand()
        }
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
        val startTime: Long = System.currentTimeMillis()
        ProcessOutputReader(process, output, commandLogger, startTime, { isAborted }, this::notifyObservers).start()
    }

}

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

private fun secondsSince(startTime: Long) = (System.currentTimeMillis() - startTime) / 1000
