package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.Dimension
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class MyAction : AnAction() {
    private val LOG = Logger.getInstance(MyAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && files != null && files.isNotEmpty()) {
            val message = Messages.showInputDialog(
                project,
                "Enter your message for aider:",
                "Aider Command",
                Messages.getQuestionIcon()
            )

            if (message != null) {
                val filePaths = files.joinToString(" ") { it.path }

                val dialog = object : DialogWrapper(project) {
                    private val textArea = JBTextArea().apply {
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                    }
                    private val scrollPane = JBScrollPane(textArea)
                    
                    init {
                        init()
                        title = "Aider Command Output"
                        setOKButtonText("Close")
                        setCancelButtonText("Cancel")
                    }

                    override fun createCenterPanel(): JComponent {
                        val panel = JPanel(BorderLayout())
                        panel.preferredSize = Dimension(600, 400)
                        panel.add(scrollPane, BorderLayout.CENTER)
                        return panel
                    }

                    fun appendText(text: String) {
                        textArea.append(text)
                        textArea.caretPosition = textArea.document.length
                    }
                }

                dialog.show()

                thread {
                    try {
                        val processBuilder = ProcessBuilder("aider", "--mini", "--file", *filePaths.split(" ").toTypedArray(), "-m", message)
                        processBuilder.redirectErrorStream(true)

                        val process = processBuilder.start()
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?

                        while (reader.readLine().also { line = it } != null) {
                            LOG.info("Aider output: $line")
                            dialog.invokeLater {
                                dialog.appendText(line + "\n")
                            }

                            if (!dialog.isShowing) {
                                process.destroy()
                                break
                            }
                        }

                        if (process.isAlive) {
                            process.waitFor()
                        }

                        val exitCode = process.exitValue()
                        dialog.invokeLater {
                            if (exitCode == 0) {
                                LOG.info("Aider command executed successfully")
                                dialog.appendText("\nAider command executed successfully.")
                            } else {
                                LOG.error("Aider command failed with exit code $exitCode")
                                dialog.appendText("\nAider command failed with exit code $exitCode.")
                            }
                        }
                    } catch (e: Exception) {
                        LOG.error("Error executing Aider command", e)
                        dialog.invokeLater {
                            dialog.appendText("\nError executing Aider command: ${e.message}")
                        }
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
