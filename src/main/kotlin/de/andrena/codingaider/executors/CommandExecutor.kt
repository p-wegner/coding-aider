package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class CommandExecutor(
    private val project: Project,
    private val commandData: CommandData
) : CommandSubject {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private val settings = AiderSettings.getInstance(project)
    private val commandLogger = CommandLogger(settings, commandData)
    private val observers = mutableListOf<CommandObserver>()

    override fun addObserver(observer: CommandObserver) {
        observers.add(observer)
    }

    override fun removeObserver(observer: CommandObserver) {
        observers.remove(observer)
    }

    override fun notifyObservers(event: (CommandObserver) -> Unit) {
        observers.forEach { event(it) }
    }

    fun executeCommand() {
        val output = StringBuilder()
        val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
        val processBuilder = ProcessBuilder(commandArgs).directory(File(project.basePath!!))
        processBuilder.environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
        processBuilder.redirectErrorStream(true)

        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")
        val initialMessage = "Starting Aider command...\n${commandLogger.getCommandString(false)}"
        notifyObservers { it.onCommandStart(initialMessage) }
        
        val process = processBuilder.start()
        pollProcessAndReadOutput(process, output)
        handleProcessCompletion(process, output)
    }

    private fun handleProcessCompletion(process: Process, output: StringBuilder) {
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            handleProcessTimeout(process, output)
        } else {
            handleProcessExit(process, output)
        }
    }

    private fun handleProcessTimeout(process: Process, output: StringBuilder) {
        process.destroy()
        val errorMessage = commandLogger.prependCommandToOutput("$output\nAider command timed out after 5 minutes")
        notifyObservers { it.onCommandError(errorMessage) }
    }

    private fun handleProcessExit(process: Process, output: StringBuilder) {
        val exitCode = process.waitFor()
        val status = if (exitCode == 0) "executed successfully" else "failed with exit code $exitCode"
        val finalOutput = commandLogger.prependCommandToOutput("$output\nAider command $status")
        notifyObservers { it.onCommandComplete(finalOutput, exitCode) }
    }

    private fun pollProcessAndReadOutput(process: Process, output: StringBuilder) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val startTime = System.currentTimeMillis()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
            val runningTime = (System.currentTimeMillis() - startTime) / 1000
            notifyObservers { it.onCommandProgress(commandLogger.prependCommandToOutput(output.toString()), runningTime) }
            if (!process.isAlive || runningTime > 300) { // 5 minutes timeout
                break
            }
            Thread.sleep(10) // Small delay to prevent UI freezing
        }
    }
}
