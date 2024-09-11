package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class SimpleExecutor(private val project: Project, private val commandData: CommandData) {
    private val logger = Logger.getInstance(SimpleExecutor::class.java)

    fun execute(): String {
        val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
        logger.info("Executing Aider command: ${commandArgs.joinToString(" ")}")

        return ProcessBuilder(commandArgs)
            .directory(File(project.basePath!!))
            .apply { 
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
            }
            .start()
            .let { process ->
                process.inputStream.bufferedReader().use { reader ->
                    reader.readText().also {
                        logger.info("Aider command completed with exit code: ${process.waitFor()}")
                    }
                }
            }
    }
}
