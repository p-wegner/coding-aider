package de.andrena.aidershortcut

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import java.awt.EventQueue.invokeLater
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class TerminalExecutor(
    private val project: Project,
    private val commandData: CommandData,
    private val files: Array<VirtualFile>
) {
    private val LOG = Logger.getInstance(TerminalExecutor::class.java)

    fun execute() {
        val output = StringBuilder()
        val markdownDialog = MarkdownDialog(project, "Aider Command Output", output.toString()).apply {
            show()
        }
        thread {
            try {
                val commandArgs = buildAiderCommand(commandData, false).split(" ")
                val processBuilder = ProcessBuilder(commandArgs)
                processBuilder.redirectErrorStream(true)

                val process = processBuilder.start()

                pollProcessAndReadOutput(process, output, markdownDialog)

                if (process.isAlive) {
                    process.destroy()
                    LOG.warn("Aider command timed out after 5 minutes")
                    invokeLater {
                        markdownDialog.updateProgress(
                            output.toString(),
                            "Aider command timed out after 5 minutes"
                        )
                    }
                } else {
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        LOG.info("Aider command executed successfully")
                        invokeLater {
                            markdownDialog.updateProgress(
                                output.toString(),
                                "Aider command executed successfully"
                            )
                        }
                    } else {
                        LOG.error("Aider command failed with exit code $exitCode")
                        invokeLater {
                            markdownDialog.updateProgress(
                                output.toString(),
                                "Aider command failed with exit code $exitCode"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error executing Aider command", e)
                invokeLater {
                    markdownDialog.updateProgress(
                        "Error executing Aider command: ${e.message}",
                        "Aider Command Error"
                    )
                }
            } finally {
                refreshFiles(files, markdownDialog, output.toString())
                markdownDialog.finish()
            }
        }
    }

    private fun buildAiderCommand(commandData: CommandData, isShellMode: Boolean): String {
        return StringBuilder("aider ${commandData.selectedCommand}").apply {
            if (commandData.filePaths.isNotBlank()) append(" --file ${commandData.filePaths}")
            if (commandData.useYesFlag) append(" --yes")
            if (!isShellMode) append(" -m \"${commandData.message}\"")
            if (commandData.readOnlyFiles.isNotEmpty()) append(" --read ${commandData.readOnlyFiles.joinToString(" ")}")
            if (commandData.additionalArgs.isNotEmpty()) append(" ${commandData.additionalArgs}")
        }.toString()
    }

    private fun refreshFiles(files: Array<VirtualFile>, markdownDialog: MarkdownDialog, output: String) {
        invokeLater {
            ApplicationManager.getApplication().invokeLater {
                WriteAction.runAndWait<Throwable> {
                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
                    RefreshQueue.getInstance().refresh(true, true, null, *files)
                }
                markdownDialog.show()
            }
        }
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
        }
    }
}
