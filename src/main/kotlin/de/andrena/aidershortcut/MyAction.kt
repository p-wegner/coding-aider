package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import java.awt.EventQueue.invokeLater
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.swing.*
import java.awt.BorderLayout
import kotlin.concurrent.thread
import com.intellij.openapi.wm.WindowManager
import javax.swing.*
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.RefreshQueue
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.ApplicationManager


class AiderAction : AnAction() {
    private val LOG = Logger.getInstance(AiderAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && files != null && files.isNotEmpty()) {
            val dialog = AiderInputDialog(project)
            if (dialog.showAndGet()) {
                val message = dialog.getInputText()
                val useYesFlag = dialog.isYesFlagChecked()
                val filePaths = files.joinToString(" ") { it.path }

                val progressDialog = ProgressDialog(project, "Aider Command in Progress")
                thread {
                    val output = StringBuilder()
                    try {
                        val commandArgs = mutableListOf("aider", "--mini", "--file")
                        commandArgs.addAll(filePaths.split(" "))
                        if (useYesFlag) {
                            commandArgs.add("--yes")
                        }
                        commandArgs.addAll(listOf("-m", message))
                        val processBuilder = ProcessBuilder(commandArgs)
                        processBuilder.redirectErrorStream(true)

                        val process = processBuilder.start()

                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?
                        val startTime = System.currentTimeMillis()

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
                                progressDialog.updateProgress(output.toString(), "Files refreshed. Aider command completed.")
                            }
                        }
                        progressDialog.finish()
                    }

                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && files != null && files.isNotEmpty()
    }
}

class AiderInputDialog(project: Project) : DialogWrapper(project) {
    private val inputTextField = JTextField(30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)

    init {
        title = "Aider Command"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.add(inputTextField)
        panel.add(yesCheckBox)
        return panel
    }

    fun getInputText(): String = inputTextField.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
}


class ProgressDialog(project: Project, title: String) {
    private val dialog: JDialog
    private val outputTextArea = JTextArea(20, 50).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val scrollPane = JScrollPane(outputTextArea)

    init {
        val projectFrame = WindowManager.getInstance().getFrame(project)
        dialog = JDialog(projectFrame, title, false) // false means non-modal
        dialog.contentPane.add(scrollPane, BorderLayout.CENTER)
        dialog.pack()
        dialog.setLocationRelativeTo(projectFrame)
        dialog.isVisible = true
    }

    fun updateProgress(output: String, message: String) {
        SwingUtilities.invokeLater {
            outputTextArea.text = output
            dialog.title = message
            outputTextArea.caretPosition = outputTextArea.document.length
        }
    }

    fun finish() {
        SwingUtilities.invokeLater {
            dialog.title = "Aider Command Completed"
            dialog.dispose()
        }
    }
}

