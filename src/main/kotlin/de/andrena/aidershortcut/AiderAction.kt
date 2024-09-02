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
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
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
                val commandData = collectCommandData(dialog, files)
                if (commandData.isShellMode) {
                    ShellExecutor(project, commandData).execute()
                } else {
                    TerminalExecutor(project, commandData, files).execute()
                }
            }
        }
    }

    private fun collectCommandData(dialog: AiderInputDialog, files: Array<VirtualFile>): CommandData {
        return CommandData(
            message = dialog.getInputText(),
            useYesFlag = dialog.isYesFlagChecked(),
            selectedCommand = dialog.getSelectedCommand(),
            additionalArgs = dialog.getAdditionalArgs(),
            filePaths = files.joinToString(" ") { it.path },
            readOnlyFiles = dialog.getReadOnlyFiles(),
            isShellMode = dialog.isShellMode()
        )
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }
}
package de.andrena.aidershortcut.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(
    name = "de.andrena.aidershortcut.settings.AiderSettingsState",
    storages = [Storage("AiderSettings.xml")]
)
@Service(Service.Level.PROJECT)
class AiderSettingsState : PersistentStateComponent<AiderSettingsState.State> {
    data class State(
        var lastTestResult: String = ""
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): AiderSettingsState =
            project.getService(AiderSettingsState::class.java)
    }
}
package de.andrena.aidershortcut.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.BufferedReader
import java.io.InputStreamReader

class AiderTestCommand(private val project: Project) {
    fun execute() {
        try {
            val process = ProcessBuilder("aider", "--help").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Messages.showInfoMessage(project, "Aider is correctly installed and accessible.", "Aider Test Result")
            } else {
                Messages.showErrorDialog(project, "Aider test failed. Exit code: $exitCode", "Aider Test Result")
            }
            
            // Save the test result
            val settingsState = AiderSettingsState.getInstance(project)
            settingsState.loadState(AiderSettingsState.State(lastTestResult = output.toString()))
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Error executing Aider test: ${e.message}", "Aider Test Error")
        }
    }
}
