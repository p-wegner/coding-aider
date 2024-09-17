package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

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
    private val outputChannel = Channel<String>(Channel.UNLIMITED)

    suspend fun startAiderProcess(commandData: CommandData) {
        val executionStrategy = getExecutionStrategy()

        val commandArgs = executionStrategy.buildCommand(commandData)
        logger.info("Starting Aider process: ${commandArgs.joinToString(" ")}")

        notifyObservers { it.onCommandStart("Starting Aider process...") }

        val processBuilder = ProcessBuilder(listOf("cmd", "/c") + commandArgs)
            .directory(File(commandData.projectPath))
            .apply {
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
                if (settings.useInteractiveMode) {
                    environment()["TERM"] = "xterm-256color"
                    environment()["COLUMNS"] = "120"
                    environment()["LINES"] = "30"
                }
            }

        executionStrategy.prepareEnvironment(processBuilder, commandData)

        process = processBuilder.start()
        inputWriter = BufferedWriter(OutputStreamWriter(process!!.outputStream, StandardCharsets.UTF_8))
        outputReader = BufferedReader(InputStreamReader(process!!.inputStream, StandardCharsets.UTF_8))

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
        inputWriter?.write(command)
        inputWriter?.newLine()
        inputWriter?.flush()
    }

    private fun startOutputReading() {
        scope.launch {
            val output = StringBuilder()
            val startTime = System.currentTimeMillis()
            val confirmationPattern =
                Pattern.compile("^Create new file\\? \\(Y\\)es/\\(N\\)o(?: \\[(Yes|No)\\])?:\\s*$")

            while (isRunning) {
                val char = outputReader?.read() ?: break
                if (char == -1) break

                output.append(char.toChar())
                val runningTime = (System.currentTimeMillis() - startTime) / 1000

                when {
                    output.endsWith("> ") -> {
                        val userResponse = notifyObserversForUserResponse(output.toString().trim(), false)
                        sendUserResponse(userResponse)
                        output.clear()
                    }

                    confirmationPattern.matcher(output).matches() -> {
                        val userResponse = notifyObserversForUserResponse("Create new file?", true)
                        sendUserResponse(userResponse)
                        output.clear()
                    }

                    char.toChar() == '\n' -> {
                        runBlocking { notifyObservers { it.onCommandProgress(output.toString(), runningTime) } }
                        outputChannel.send(output.toString())
                        output.clear()
                    }
                }
            }
            outputChannel.close()
        }
    }

    private suspend fun notifyObserversForUserResponse(prompt: String, isConfirmation: Boolean): UserResponse {
        return withContext(Dispatchers.Default) {
            val deferreds = mutableListOf<Deferred<UserResponse>>()
            notifyObservers { observer ->
                val deferred = async {
                    if (isConfirmation) {
                        val result = observer.onUserConfirmationRequired(prompt).await()
                        UserResponse.Confirmation(result)
                    } else {
                        val result = observer.onUserInputRequired(prompt).await()
                        result?.let { UserResponse.Input(it) } ?: UserResponse.NoResponse
                    }
                }
                deferreds.add(deferred)
            }

            try {
                withTimeout(5000) {
                    select<UserResponse> {
                        deferreds.forEach { deferred ->
                            deferred.onAwait { it }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                if (isConfirmation) UserResponse.Confirmation(false) else throw TimeoutException("User input timed out")
            } finally {
                // Cancel all other deferreds
                deferreds.forEach { it.cancel() }
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
            inputWriter?.write(responseText)
            inputWriter?.newLine()
            inputWriter?.flush()
        }
    }

    suspend fun stopAiderProcess() {
        isRunning = false
        inputWriter?.close()
        outputReader?.close()
        outputChannel.close()
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
