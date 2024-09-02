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
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalView
import org.jetbrains.plugins.terminal.TerminalViewFactory // Added import
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
            val dialog = AiderInputDialog(project, files.map { it.path })
            if (dialog.showAndGet()) {
                val message = dialog.getInputText()
                val useYesFlag = dialog.isYesFlagChecked()
                val selectedCommand = dialog.getSelectedCommand()
                val additionalArgs = dialog.getAdditionalArgs()
                val filePaths = files.joinToString(" ") { it.path }
                val readOnlyFiles = dialog.getReadOnlyFiles()
                val isShellMode = dialog.isShellMode()

                if (isShellMode) {
                    executeInTerminal(project, useYesFlag, selectedCommand, additionalArgs, filePaths, readOnlyFiles)
                } else {
                    executeWithProgressDialog(project, message, useYesFlag, selectedCommand, additionalArgs, filePaths, readOnlyFiles, files)
                }
            }
        }
    }

    private fun executeInTerminal(
        project: Project,
        useYesFlag: Boolean,
        selectedCommand: String,
        additionalArgs: String,
        filePaths: String,
        readOnlyFiles: List<String>
    ) {
        val terminalView = TerminalView.getInstance(project)
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val terminalToolWindow = toolWindowManager.getToolWindow("Terminal")

        terminalToolWindow?.show {
            val terminal = terminalView.createLocalShellWidget(project.basePath, "Aider")
            val shellTerminal = terminal as? ShellTerminalWidget ?: return@show

            val command = buildAiderCommand("", useYesFlag, selectedCommand, additionalArgs, filePaths, readOnlyFiles, true)
            shellTerminal.executeCommand(command)
        }
    }

    private fun executeWithProgressDialog(
        project: Project,
        message: String,
        useYesFlag: Boolean,
        selectedCommand: String,
        additionalArgs: String,
        filePaths: String,
        readOnlyFiles: List<String>,
        files: Array<VirtualFile>
    ) {
        val progressDialog = ProgressDialog(project, "Aider Command in Progress")
        thread {
            val output = StringBuilder()
            try {
                val commandArgs = buildAiderCommand(message, useYesFlag, selectedCommand, additionalArgs, filePaths, readOnlyFiles, false).split(" ")
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

    private fun buildAiderCommand(
        message: String,
        useYesFlag: Boolean,
        selectedCommand: String,
        additionalArgs: String,
        filePaths: String,
        readOnlyFiles: List<String>,
        isShellMode: Boolean
    ): String {
        val command = StringBuilder("aider $selectedCommand --file $filePaths")
        if (useYesFlag) {
            command.append(" --yes")
        }
        if (!isShellMode) {
            command.append(" -m \"$message\"")
        }
        if (readOnlyFiles.isNotEmpty()) {
            command.append(" --read ${readOnlyFiles.joinToString(" ")}")
        }
        if (additionalArgs.isNotEmpty()) {
            command.append(" $additionalArgs")
        }
        return command.toString()
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
