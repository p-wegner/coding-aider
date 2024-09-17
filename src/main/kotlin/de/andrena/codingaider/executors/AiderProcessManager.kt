package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import java.io.*
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

    suspend fun startAiderProcess(commandData: CommandData) {

        val executionStrategy = getExecutionStrategy()

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

    private fun getExecutionStrategy() =
        when (settings.useInteractiveMode to settings.useDockerAider) {
            (true to false) -> NativeInteractiveAiderExecutionStrategy(apiKeyChecker, settings)
            (false to true) -> DockerAiderExecutionStrategy(dockerManager, apiKeyChecker, settings)
            else -> NativeAiderExecutionStrategy(apiKeyChecker, settings)
        }

    fun sendCommand(command: String): Unit {
        inputWriter?.write(command)
        inputWriter?.newLine()
        inputWriter?.flush()

        startOutputReading()
    }

    private fun startOutputReading() {
        scope.launch {
            val output = StringBuilder()
            var line: String? = null
            val startTime = System.currentTimeMillis()
            val confirmationPattern =
                Pattern.compile("^Create new file\\? \\(Y\\)es/\\(N\\)o(?: \\[(Yes|No)\\])?:\\s*$")
            while (isRunning && outputReader?.readLine().also { line = it } != null) {
                val runningTime = (System.currentTimeMillis() - startTime) / 1000
                if (line == "> ") {
                    val userResponse = notifyObserversForUserResponse(output.toString(), false)
                    sendUserResponse(userResponse)
                    output.clear()
                    continue
                }
                if (confirmationPattern.matcher(line ?: "").matches()) {
                    val userResponse = notifyObserversForUserResponse("Create new file?", true)
                    sendUserResponse(userResponse)
                    continue
                }
                output.append(line).append("\n")
                runBlocking { notifyObservers { it.onCommandProgress(output.toString(), runningTime) } }
            }
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
                        result?.let { UserResponse.Input(it) } ?: throw CancellationException("No input provided")
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
