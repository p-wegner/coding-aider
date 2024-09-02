package de.andrena.aidershortcut

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger
import java.awt.EventQueue.invokeLater
import java.io.BufferedReader
import java.io.InputStreamReader

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

                try {
                    val processBuilder = ProcessBuilder("aider", "--mini", "--file", *filePaths.split(" ").toTypedArray(), "-m", message)
                    processBuilder.redirectErrorStream(true)

                        val process = processBuilder.start()
                    val output = StringBuilder()

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    var lastUpdateTime = System.currentTimeMillis()
                    val updateInterval = 1000 // Update UI every 1 second
                var runningTime = 0L

                    while (reader.readLine().also { line = it } != null) {
                            output.append(line).append("\n")
                                    LOG.info("Aider output: $line")

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime > updateInterval) {
                        runningTime += (currentTime - lastUpdateTime)
                                    invokeLater {
                            Messages.showInfoMessage(project, "Aider command in progress (${runningTime / 1000} seconds):\n\n$output", "Aider Command")
                            }
                            lastUpdateTime = currentTime
                        }

                    // Check if the process has finished or if it's been running for too long
                    if (!process.isAlive || runningTime > 300000) { // 5 minutes timeout
                        break
                    }
                }

                if (process.isAlive) {
                    process.destroy()
                    LOG.warn("Aider command timed out after 5 minutes")
                    Messages.showWarningDialog(project, "Aider command timed out after 5 minutes.\n\nPartial Output:\n$output", "Aider Command Timeout")
                    } else {
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        LOG.info("Aider command executed successfully")
                        Messages.showInfoMessage(project, "Aider command executed successfully.\n\nFinal Output:\n$output", "Aider Command")
                    } else {
                        LOG.error("Aider command failed with exit code $exitCode")
                        Messages.showErrorDialog(project, "Aider command failed with exit code $exitCode.\n\nFinal Output:\n$output", "Aider Command Error")
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error executing Aider command", e)
                Messages.showErrorDialog(project, "Error executing Aider command: ${e.message}", "Aider Command Error")
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
