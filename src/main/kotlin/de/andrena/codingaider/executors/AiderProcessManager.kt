package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import kotlinx.coroutines.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class AiderProcessManager(
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) : CommandSubject by GenericCommandSubject() {
    private val logger = Logger.getInstance(AiderProcessManager::class.java)
    private val settings = AiderSettings.getInstance(project)
    var ttyConnector: ReactiveProcessTtyConnector? = null
    private val dockerManager = DockerContainerManager()
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Default)

    suspend fun startAiderProcess(commandData: CommandData) {
        if (isRunning) {
            logger.info("Aider process is already running")
            return
        }

        val executionStrategy = getExecutionStrategy()
        val commandArgs = executionStrategy.buildCommand(commandData) + listOf("--no-pretty")
        logger.info("Starting Aider process: ${commandArgs.joinToString(" ")}")

        notifyObservers { it.onCommandStart("Starting Aider process...") }

        val processBuilder = ProcessBuilder(commandArgs)
            .directory(File(commandData.projectPath))
            .apply {
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
                if (settings.useInteractiveMode && !System.getProperty("os.name").lowercase().contains("win")) {
//                    environment()["TERM"] = "xterm-256color"
//                    environment()["COLUMNS"] = "120"
//                    environment()["LINES"] = "30"
                }
            }

        executionStrategy.prepareEnvironment(processBuilder, commandData)

        val process = processBuilder.start()
        ttyConnector = ReactiveProcessTtyConnector(process, StandardCharsets.UTF_8)

        isRunning = true
        startOutputReading()
    }

    private fun getExecutionStrategy() =
        when (settings.useInteractiveMode to settings.useDockerAider) {
            (true to false) -> NativeInteractiveAiderExecutionStrategy(apiKeyChecker, settings)
            (false to true) -> DockerAiderExecutionStrategy(dockerManager, apiKeyChecker, settings)
            else -> NativeAiderExecutionStrategy(apiKeyChecker, settings)
        }

    fun sendCommand(command: String) {
        ttyConnector?.write("$command\n")
    }

    private fun startOutputReading() {
        scope.launch {
            val output = StringBuilder()
            val fullOutput = StringBuilder()
            val startTime = System.currentTimeMillis()
            val confirmationPattern =
                Pattern.compile("^Create new file\\? \\(Y\\)es/\\(N\\)o(?: \\[(Yes|No)\\])?:\\s*$")
            val buffer = CharArray(1024)

            while (isRunning) {
                val readCount = ttyConnector?.read(buffer, 0, buffer.size) ?: break
                if (readCount == -1) break

                output.append(buffer, 0, readCount)
                val runningTime = (System.currentTimeMillis() - startTime) / 1000

                when {
                    output.toString().endsWith("> ") -> {
                        runBlocking { notifyObservers { it.onCommandProgress(output.toString(), runningTime) } }
                        val userResponse = notifyObserversForUserResponse(output.toString().trim(), false)
                        sendUserResponse(userResponse)
                        output.clear()
                        fullOutput.clear()
                    }

                    confirmationPattern.matcher(output).matches() -> {
                        runBlocking { notifyObservers { it.onCommandProgress(output.toString(), runningTime) } }
                        val userResponse = notifyObserversForUserResponse("Create new file?", true)
                        sendUserResponse(userResponse)
                        output.clear()
                        fullOutput.clear()
                    }

                    output.toString().contains('\n') -> {
                        val lines = output.toString().split('\n')
                        for (i in 0 until lines.size - 1) {
                            fullOutput.append(lines[i]).append('\n')
                            runBlocking { notifyObservers { it.onCommandProgress(fullOutput.toString(), runningTime) } }
                        }
                        output.clear()
                        output.append(lines.last())
                    }
                }
            }
        }
    }

    private suspend fun notifyObserversForUserResponse(prompt: String, isConfirmation: Boolean): UserResponse {
        return withContext(Dispatchers.Default) {
            val responses = mutableListOf<UserResponse>()
            notifyObservers { observer ->
                val response = if (isConfirmation) {
                    observer.onUserConfirmationRequired(prompt).await()?.let { UserResponse.Confirmation(it) }
                } else {
                    observer.onUserInputRequired(prompt).await()?.let { UserResponse.Input(it) }
                }
                response?.let { responses.add(it) }
            }

            try {
                withTimeout(5000) {
                    responses.firstOrNull() ?: if (isConfirmation) {
                        UserResponse.Confirmation(false)
                    } else {
                        throw TimeoutException("No valid user input received")
                    }
                }
            } catch (e: TimeoutCancellationException) {
                if (isConfirmation) UserResponse.Confirmation(false) else throw TimeoutException("User input timed out")
            }
        }
    }

    private suspend fun sendUserResponse(response: UserResponse) {
        val responseText = when (response) {
            is UserResponse.Confirmation -> if (response.value) "y" else "n"
            is UserResponse.Input -> response.value
            is UserResponse.NoResponse -> return
        }
        withContext(Dispatchers.IO) {
            ttyConnector?.write("$responseText\n")
        }
    }

    suspend fun stopAiderProcess() {
        isRunning = false
        ttyConnector?.close()
        ttyConnector = null
        if (settings.useDockerAider) {
            dockerManager.stopDockerContainer()
        }
        notifyObservers { it.onCommandComplete("Aider process stopped", 0) }
        scope.cancel()
    }

    fun isProcessRunning(): Boolean = isRunning
}
