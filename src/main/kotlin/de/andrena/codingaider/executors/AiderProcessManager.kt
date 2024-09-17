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
            val confirmationPattern = Pattern.compile("^Create new file\\? \\(Y\\)es/\\(N\\)o(?: \\[(Yes|No)\\])?:\\s*$")
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
                notifyObservers { it.onCommandProgress(output.toString(), runningTime) }
            }
        }
    }

    private suspend fun notifyObserversForUserResponse(prompt: String, isConfirmation: Boolean): UserResponse {
        val responseChannel = Channel<UserResponse>()
        
        notifyObservers { observer ->
            launch {
                val response = if (isConfirmation) {
                    UserResponse(confirmation = observer.onUserConfirmationRequired(prompt).await())
                } else {
                    UserResponse(input = observer.onUserInputRequired(prompt).await())
                }
                responseChannel.send(response)
            }
        }

        return select {
            responseChannel.onReceive { it }
            onTimeout(5000) { UserResponse() }
        }
    }

    private suspend fun sendUserResponse(response: UserResponse) {
        val responseText = when {
            response.confirmation != null -> if (response.confirmation) "y" else "n"
            response.input != null -> response.input
            else -> "n"  // Default to "no" if no response is received
        }
        withContext(Dispatchers.IO) {
            inputWriter?.write(responseText)
            inputWriter?.newLine()
            inputWriter?.flush()
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
