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

class CommandExecutor(private val project: Project, private val commandData: CommandData) : CommandSubject {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private val settings = AiderSettings.getInstance(project)
    private val commandLogger = CommandLogger(settings, commandData)
    private val observers = mutableListOf<CommandObserver>()

    override fun addObserver(observer: CommandObserver) = observers.add(observer)
    override fun removeObserver(observer: CommandObserver) = observers.remove(observer)
    override fun notifyObservers(event: (CommandObserver) -> Unit) = observers.forEach(event)

    fun executeCommand() {
        val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")
        notifyObservers { it.onCommandStart("Starting Aider command...\n${commandLogger.getCommandString(false)}") }
        
        val process = ProcessBuilder(commandArgs)
            .directory(File(project.basePath!!))
            .apply { 
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
            }
            .start()

        val output = StringBuilder()
        pollProcessAndReadOutput(process, output)
        handleProcessCompletion(process, output)
    }

    private fun handleProcessCompletion(process: Process, output: StringBuilder) {
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroy()
            notifyObservers { it.onCommandError(commandLogger.prependCommandToOutput("$output\nAider command timed out after 5 minutes")) }
        } else {
            val exitCode = process.exitValue()
            val status = if (exitCode == 0) "executed successfully" else "failed with exit code $exitCode"
            notifyObservers { it.onCommandComplete(commandLogger.prependCommandToOutput("$output\nAider command $status"), exitCode) }
        }
    }

    private fun pollProcessAndReadOutput(process: Process, output: StringBuilder) {
        val startTime = System.currentTimeMillis()
        process.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                output.append(line).append("\n")
                val runningTime = (System.currentTimeMillis() - startTime) / 1000
                notifyObservers { it.onCommandProgress(commandLogger.prependCommandToOutput(output.toString()), runningTime) }
                if (!process.isAlive || runningTime > 300) break
                Thread.sleep(10)
            }
        }
    }
}
