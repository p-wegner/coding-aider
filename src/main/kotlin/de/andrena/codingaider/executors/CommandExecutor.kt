package de.andrena.codingaider.executors

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.executors.api.AiderProcessInteractor
import de.andrena.codingaider.executors.api.CommandSubject
import de.andrena.codingaider.executors.api.DefaultAiderProcessInteractor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileExtractorService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.io.File
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

        val fileExtractorService = FileExtractorService.getInstance()
        val extractedFiles = fileExtractorService.extractFilesIfNeeded(commandData.files)
        val updatedCommandData = commandData.copy(files = extractedFiles)

        // Check if sidecar mode is enabled
        if (settings.useSidecarMode) {
            return executeSidecarCommand(updatedCommandData)
        }

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

        val output = try {
            val response = processInteractor.sendCommand(commandString)
            val outputState = processInteractor.parseOutput(response)

            if (outputState.hasError) {
                notifyObservers { it.onCommandError(response) }
                response
            } else {
                notifyObservers { it.onCommandComplete(response, 0) }
                response
            }
        } catch (e: Exception) {
            val errorMessage = "Sidecar command failed: ${e.message}"
            logger.error(errorMessage, e)
            notifyObservers { it.onCommandError(errorMessage) }
            errorMessage
        }

        return commandLogger.prependCommandToOutput(output)
    }

    private fun buildSidecarCommandString(commandData: CommandData): String {
        val fileList = commandData.files.joinToString(" ") { it.filePath }
        return when (commandData.aiderMode) {
            AiderMode.NORMAL -> "edit $fileList -m \"${commandData.message}\""
            AiderMode.STRUCTURED -> "edit $fileList -m \"${
                project.service<AiderPlanService>().createAiderPlanSystemPrompt(commandData)
            }\""

            AiderMode.ARCHITECT -> "edit $fileList -m \"/architect ${commandData.message}\""
            AiderMode.SHELL -> "edit $fileList"
            else -> "edit $fileList"
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
