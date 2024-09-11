package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class SimpleExecutor(
    private val project: Project,
    private val commandData: CommandData
) {
    private val logger = Logger.getInstance(SimpleExecutor::class.java)

    fun execute(): String {
        val output = StringBuilder()
        val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
        val processBuilder = ProcessBuilder(commandArgs).directory(File(project.basePath!!))
        processBuilder.environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
        processBuilder.redirectErrorStream(true)

        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        val exitCode = process.waitFor()
        logger.info("Aider command completed with exit code: $exitCode")

        return output.toString()
    }
}
