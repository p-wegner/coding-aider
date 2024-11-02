package de.andrena.codingaider.executors.api

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.CommandExecutor
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.services.AiderPlanService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import de.andrena.codingaider.utils.GitUtils
import java.awt.EventQueue.invokeLater
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class IDEBasedExecutor(
    private val project: Project,
    private val commandData: CommandData
) : CommandObserver, Abortable {
    private val log = Logger.getInstance(IDEBasedExecutor::class.java)
    private lateinit var markdownDialog: MarkdownDialog
    private var currentCommitHash: String? = null
    private val commandExecutor = AtomicReference<CommandExecutor?>(null)
    private var executionThread: Thread? = null
    private var isFinished: CountDownLatch = CountDownLatch(1)
    private var initialPlanFiles: Set<File> = emptySet()

    fun execute(): MarkdownDialog {
        markdownDialog = MarkdownDialog(
            project,
            "Aider Command Output",
            "Initializing Aider command...",
            this
        )
            .apply {
                isVisible = true
                focus()
            }
        currentCommitHash = GitUtils.getCurrentCommitHash(project)

        val plansFolder = File(project.basePath, AiderPlanService.AIDER_PLANS_FOLDER)
        if (plansFolder.exists() && plansFolder.isDirectory) {
            initialPlanFiles = plansFolder.listFiles { file -> file.isFile && file.extension == "md" }
                ?.toSet() ?: emptySet()
        }

        executionThread = thread {
            executeAiderCommand()
            isFinished.countDown()
        }
        return markdownDialog
    }
    fun isFinished(): CountDownLatch = isFinished
    private fun executeAiderCommand() {
        try {
            val executor = CommandExecutor(commandData, project).apply {
                addObserver(this@IDEBasedExecutor)
            }
            commandExecutor.set(executor)
            executor.executeCommand()
        } catch (e: Exception) {
            log.error("Error executing Aider command", e)
            updateDialogProgress("Error executing Aider command: ${e.message}", "Aider Command Error")
        }
    }

    override fun abortCommand() {
        commandExecutor.get()?.abortCommand()
        executionThread?.interrupt()
        updateDialogProgress("Aider command aborted by user", "Aider Command Aborted")
        markdownDialog.dispose()
    }

    private fun refreshFiles() {
        val files =
            commandData.files.mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it.filePath) }.toTypedArray()
        FileRefresher.refreshFiles(files, markdownDialog)
    }

    private fun openGitComparisonToolIfNeeded() {
        if (getInstance().showGitComparisonTool) {
            currentCommitHash?.let { GitUtils.openGitComparisonTool(project, it) { markdownDialog.focus(1000) } }
        }
    }

    private fun updateDialogProgress(message: String, title: String) {
        invokeLater { markdownDialog.updateProgress(message, title) }
    }

    private fun addNewPlanFilesToPersistentFiles() {
        if (commandData.structuredMode) {
            val plansFolder = File(project.basePath, AiderPlanService.AIDER_PLANS_FOLDER)
            if (plansFolder.exists() && plansFolder.isDirectory) {
                val currentPlanFiles = plansFolder.listFiles { file -> file.isFile && file.extension == "md" }
                    ?.toSet() ?: emptySet()

                val newPlanFiles = currentPlanFiles.subtract(initialPlanFiles)
                newPlanFiles.forEach { file ->
                    PersistentFileService.getInstance(project).addFile(FileData(file.absolutePath, false))
                }
            }
        }
    }

    override fun onCommandStart(message: String) =
        updateDialogProgress(message, "Aider Command Started")

    override fun onCommandProgress(message: String, runningTime: Long) =
        updateDialogProgress(message, "Aider command in progress ($runningTime seconds)")

    override fun onCommandComplete(message: String, exitCode: Int) {
        updateDialogProgress(message, "Aider Command ${if (exitCode == 0) "Completed" else "Failed"}")
        markdownDialog.startAutoCloseTimer()
        refreshFiles()
        addNewPlanFilesToPersistentFiles()
        openGitComparisonToolIfNeeded()
        if (!getInstance().closeOutputDialogImmediately) {
            markdownDialog.setProcessFinished()
            markdownDialog.focus()
        }
    }

    override fun onCommandError(message: String) {
        updateDialogProgress(message, "Aider Command Error")
        markdownDialog.setProcessFinished()
        markdownDialog.startAutoCloseTimer()
    }
}
