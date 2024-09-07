package de.andrena.codingaider.executors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.AiderCommandBuilder
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import java.awt.EventQueue.invokeLater
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.concurrent.thread

class IDEBasedExecutor(
    private val project: Project,
    private val commandData: CommandData
) {
    private val LOG = Logger.getInstance(IDEBasedExecutor::class.java)

    fun execute() {
        val output = StringBuilder()
        val markdownDialog = MarkdownDialog(project, "Aider Command Output", "Initializing Aider command...").apply {
            isVisible = true
        }
        val currentCommitHash = GitUtils.getCurrentCommitHash(project)

        thread {
            try {
                val commandArgs = AiderCommandBuilder.buildAiderCommand(commandData, false)
                val processBuilder = ProcessBuilder(commandArgs).directory(File(project.basePath!!))
                processBuilder.redirectErrorStream(true)

                invokeLater {
                    markdownDialog.updateProgress("Starting Aider command...\n", "Aider Command In Progress")
                }

                val process = processBuilder.start()

                pollProcessAndReadOutput(process, output, markdownDialog)

                if (process.isAlive) {
                    process.destroy()
                    LOG.warn("Aider command timed out after 5 minutes")
                    invokeLater {
                        markdownDialog.updateProgress(
                            "$output\nAider command timed out after 5 minutes",
                            "Aider Command Timed Out"
                        )
                    }
                } else {
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        LOG.info("Aider command executed successfully")
                        invokeLater {
                            markdownDialog.updateProgress(
                                "$output\nAider command executed successfully",
                                "Aider Command Completed"
                            )
                        }
                    } else {
                        LOG.error("Aider command failed with exit code $exitCode")
                        invokeLater {
                            markdownDialog.updateProgress(
                                "$output\nAider command failed with exit code $exitCode",
                                "Aider Command Failed"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error executing Aider command", e)
                invokeLater {
                    markdownDialog.updateProgress(
                        output.toString() + "\nError executing Aider command: ${e.message}",
                        "Aider Command Error"
                    )
                }
            } finally {
                refreshFiles(commandData.files.mapNotNull {
                    VirtualFileManager.getInstance().findFileByUrl(it.filePath)
                }.toTypedArray(), markdownDialog)

                // Open the git comparison tool
                invokeLater {
                    currentCommitHash?.let { GitUtils.openGitComparisonTool(project, it) }
                }
            }
        }
    }

    private fun refreshFiles(files: Array<VirtualFile>, markdownDialog: MarkdownDialog) {
        FileRefresher.refreshFiles(project, files, markdownDialog)
    }

    private fun pollProcessAndReadOutput(
        process: Process,
        output: StringBuilder,
        markdownDialog: MarkdownDialog
    ) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val startTime = System.currentTimeMillis()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
            LOG.info("Aider output: $line")
            val runningTime = (System.currentTimeMillis() - startTime) / 1000
            invokeLater {
                markdownDialog.updateProgress(
                    output.toString(),
                    "Aider command in progress ($runningTime seconds)"
                )
            }
            if (!process.isAlive || runningTime > 300) { // 5 minutes timeout
                break
            }
            Thread.sleep(100) // Small delay to prevent UI freezing
        }
    }
}
