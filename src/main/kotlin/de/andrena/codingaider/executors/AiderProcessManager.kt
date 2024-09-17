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
import kotlinx.coroutines.*
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
            val confirmationPattern = Pattern.compile("^Do you want to (.*?)\\? \\[y/n\\]:\\s*$")
            while (isRunning && outputReader?.readLine().also { line = it } != null) {
                val runningTime = (System.currentTimeMillis() - startTime) / 1000
                if (line == "> ") {
                    notifyObservers { it.onUserInputRequired(output.toString()) }
                    break
                }
                val matcher = confirmationPattern.matcher(line ?: "")
                if (matcher.find()) {
                    val confirmationPrompt = matcher.group(1)
                    val userConfirmation = notifyObserversForConfirmation(confirmationPrompt)
                    sendConfirmation(userConfirmation)
                    continue
                }
                output.append(line).append("\n")
                notifyObservers { it.onCommandProgress(output.toString(), runningTime) }
            }
        }
    }

    private suspend fun notifyObserversForConfirmation(prompt: String): Boolean = withContext(Dispatchers.Main) {
        var confirmation = false
        notifyObservers { observer ->
            confirmation = observer.onUserConfirmationRequired(prompt)
        }
        confirmation
    }

    private fun sendConfirmation(confirmed: Boolean) {
        val response = if (confirmed) "y" else "n"
        inputWriter?.write(response)
        inputWriter?.newLine()
        inputWriter?.flush()
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
