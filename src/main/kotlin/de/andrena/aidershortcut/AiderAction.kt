package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
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

class AiderAction : AnAction() {
    private val LOG = Logger.getInstance(AiderAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val dialog = AiderInputDialog(project)
            if (dialog.showAndGet()) {
                val message = dialog.getInputText()
                val useYesFlag = dialog.isYesFlagChecked()
                val selectedCommand = dialog.getSelectedCommand()
                val filePaths = files.joinToString(" ") { it.path }

                val progressDialog = ProgressDialog(project, "Aider Command in Progress")
                thread {
                    val output = StringBuilder()
                    try {
                        val commandArgs = mutableListOf("aider", selectedCommand, "--file")
                        commandArgs.addAll(filePaths.split(" "))
                        if (useYesFlag) {
                            commandArgs.add("--yes")
                        }
                        commandArgs.addAll(listOf("-m", message))
                        val processBuilder = ProcessBuilder(commandArgs)
                        processBuilder.redirectErrorStream(true)

                        val process = processBuilder.start()

                        pollProcessAndReadOutput(process, output, progressDialog)

                        if (process.isAlive) {
                            process.destroy()
                            LOG.warn("Aider command timed out after 5 minutes")
                            invokeLater {
                                progressDialog.updateProgress(
                                    output.toString(),
                                    "Aider command timed out after 5 minutes"
                                )
                            }
                        } else {
                            val exitCode = process.waitFor()
                            if (exitCode == 0) {
                                LOG.info("Aider command executed successfully")
                                invokeLater {
                                    progressDialog.updateProgress(
                                        output.toString(),
                                        "Aider command executed successfully"
                                    )
                                }
                            } else {
                                LOG.error("Aider command failed with exit code $exitCode")
                                invokeLater {
                                    progressDialog.updateProgress(
                                        output.toString(),
                                        "Aider command failed with exit code $exitCode"
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        LOG.error("Error executing Aider command", e)
                        invokeLater {
                            progressDialog.updateProgress(
                                "Error executing Aider command: ${e.message}",
                                "Aider Command Error"
                            )
                        }
                    } finally {
                        invokeLater {
                            ApplicationManager.getApplication().invokeLater {
                                WriteAction.runAndWait<Throwable> {
                                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(false)
                                    RefreshQueue.getInstance().refresh(true, true, null, *files)
                                }
                                progressDialog.updateProgress(
                                    output.toString(),
                                    "Files refreshed. Aider command completed."
                                )
                            }
                        }
                        progressDialog.finish()
                    }

                }
            }
        }
    }

    private fun pollProcessAndReadOutput(
        process: Process,
        output: StringBuilder,
        progressDialog: ProgressDialog
    ) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val startTime = System.currentTimeMillis()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
            LOG.info("Aider output: $line")
            val runningTime = (System.currentTimeMillis() - startTime) / 1000
            invokeLater {
                progressDialog.updateProgress(
                    output.toString(),
                    "Aider command in progress ($runningTime seconds)"
                )
            }
            if (!process.isAlive || runningTime > 300) { // 5 minutes timeout
                break
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }
}
