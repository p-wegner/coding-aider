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

class CommandExecutor(
    private val project: Project,
    private val commandData: CommandData,
    private val markdownDialog: MarkdownDialog
) {
    private val logger = Logger.getInstance(CommandExecutor::class.java)
    private val settings = AiderSettings.getInstance(project)

    fun executeCommand() {
        val output = StringBuilder()
        val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
        val processBuilder = ProcessBuilder(commandArgs).directory(File(project.basePath!!))
        processBuilder.redirectErrorStream(true)

        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")
        logger.info("Using JVM default encoding: ${System.getProperty("file.encoding")}")
        
        val initialMessage = if (settings.verboseCommandLogging) {
            "Starting Aider command...\nCommand: ${commandArgs.joinToString(" ")}\n"
        } else {
            "Starting Aider command...\n"
        }
        updateDialogProgress(initialMessage, "Aider Command In Progress")

        val process = processBuilder.start()
        pollProcessAndReadOutput(process, output)
        handleProcessCompletion(process, output)
    }

    private fun handleProcessCompletion(process: Process, output: StringBuilder) {
        if (process.isAlive) {
            handleProcessTimeout(process, output)
        } else {
            handleProcessExit(process, output)
        }
    }

    private fun handleProcessTimeout(process: Process, output: StringBuilder) {
        process.destroy()
        val commandString = getCommandString()
        updateDialogProgress("$commandString$output\nAider command timed out after 5 minutes", "Aider Command Timed Out")
    }

    private fun handleProcessExit(process: Process, output: StringBuilder) {
        val exitCode = process.waitFor()
        val commandString = getCommandString()
        if (exitCode == 0) {
            updateDialogProgress("$commandString$output\nAider command executed successfully", "Aider Command Completed")
        } else {
            updateDialogProgress("$commandString$output\nAider command failed with exit code $exitCode", "Aider Command Failed")
        }
    }

    private fun getCommandString(): String {
        return if (settings.verboseCommandLogging) {
            val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
            "Command: ${commandArgs.joinToString(" ")}\n\n"
        } else {
            ""
        }
    }

    private fun updateDialogProgress(message: String, title: String) {
        markdownDialog.updateProgress(message, title)
    }

    private fun pollProcessAndReadOutput(process: Process, output: StringBuilder) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val startTime = System.currentTimeMillis()
        val commandString = getCommandString()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
            val runningTime = (System.currentTimeMillis() - startTime) / 1000
            updateDialogProgress(commandString + output.toString(), "Aider command in progress ($runningTime seconds)")
            if (!process.isAlive || runningTime > 300) { // 5 minutes timeout
                break
            }
            Thread.sleep(10) // Small delay to prevent UI freezing
        }
    }
}
