package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.MarkdownDialog
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import com.intellij.openapi.diagnostic.Logger
import de.andrena.codingaider.settings.AiderSettings
import java.util.concurrent.TimeUnit

class CommandExecutor(
    private val project: Project,
    private val commandData: CommandData,
    private val markdownDialog: MarkdownDialog
) {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private val settings = AiderSettings.getInstance(project)
    private val commandLogger = CommandLogger(settings, commandData)

    private fun getShellCommand(): List<String> {
        return when {
            System.getProperty("os.name").toLowerCase().contains("win") -> listOf("cmd.exe", "/c")
            else -> listOf("bash", "-c")
        }
    }

    fun executeCommand() {
        val output = StringBuilder()
        val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
        val shellCommand = getShellCommand() + listOf(commandArgs.joinToString(" "))
        val processBuilder = ProcessBuilder(shellCommand).directory(File(project.basePath!!))
        processBuilder.environment()["PYTHONIOENCODING"] = "utf-8"
        processBuilder.redirectErrorStream(true)

        logger.info("Executing Aider command: ${shellCommand.joinToString(" ")}")
        logger.info("Using JVM default encoding: ${System.getProperty("file.encoding")}")
        
        val initialMessage = "Starting Aider command...\n${commandLogger.getCommandString(false)}"
        updateDialogProgress(initialMessage, "Aider Command In Progress")

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
        updateDialogProgress(commandLogger.prependCommandToOutput("$output\nAider command timed out after 5 minutes"), "Aider Command Timed Out")
    }

    private fun handleProcessExit(process: Process, output: StringBuilder) {
        val exitCode = process.waitFor()
        val status = if (exitCode == 0) "executed successfully" else "failed with exit code $exitCode"
        updateDialogProgress(commandLogger.prependCommandToOutput("$output\nAider command $status"), "Aider Command ${if (exitCode == 0) "Completed" else "Failed"}")
    }

    private fun updateDialogProgress(message: String, title: String) {
        markdownDialog.updateProgress(message, title)
    }

    private fun pollProcessAndReadOutput(process: Process, output: StringBuilder) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val startTime = System.currentTimeMillis()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
            val runningTime = (System.currentTimeMillis() - startTime) / 1000
            updateDialogProgress(commandLogger.prependCommandToOutput(output.toString()), "Aider command in progress ($runningTime seconds)")
            if (!process.isAlive || runningTime > 300) { // 5 minutes timeout
                break
            }
            Thread.sleep(10) // Small delay to prevent UI freezing
        }
    }
}
