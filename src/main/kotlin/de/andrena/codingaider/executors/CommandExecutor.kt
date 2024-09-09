package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.MarkdownDialog
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import com.intellij.openapi.diagnostic.Logger

class CommandExecutor(
    private val project: Project,
    private val commandData: CommandData,
    private val markdownDialog: MarkdownDialog
) {
    private val logger = Logger.getInstance(CommandExecutor::class.java)

    fun executeCommand() {
        val output = StringBuilder()
        val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
        val processBuilder = ProcessBuilder(commandArgs).directory(File(project.basePath!!))
        processBuilder.redirectErrorStream(true)

        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")
        updateDialogProgress("Starting Aider command...\n", "Aider Command In Progress")

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
        updateDialogProgress("$output\nAider command timed out after 5 minutes", "Aider Command Timed Out")
    }

    private fun handleProcessExit(process: Process, output: StringBuilder) {
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            updateDialogProgress("$output\nAider command executed successfully", "Aider Command Completed")
        } else {
            updateDialogProgress("$output\nAider command failed with exit code $exitCode", "Aider Command Failed")
        }
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
            updateDialogProgress(output.toString(), "Aider command in progress ($runningTime seconds)")
            if (!process.isAlive || runningTime > 300) { // 5 minutes timeout
                break
            }
            Thread.sleep(10) // Small delay to prevent UI freezing
        }
    }
}
