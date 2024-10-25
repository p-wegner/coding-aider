package de.andrena.codingaider.actions.aider

import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import de.andrena.codingaider.actions.aider.FixGradleErrorActionGroup.Companion.hasGradleErrors
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData

class FixGradleErrorActionGroup : ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            FixGradleErrorAction(),
            FixGradleErrorInteractive()
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val hasErrors = project != null && hasGradleErrors(project)
        e.presentation.isEnabledAndVisible = hasErrors
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun hasGradleErrors(project: Project): Boolean {
            return RunContentManager.getInstance(project).allDescriptors.all {
                return it.processHandler?.exitCode != 0
            }
        }
    }
}

abstract class BaseFixGradleErrorAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val hasErrors = project != null && hasGradleErrors(project)
        e.presentation.isEnabledAndVisible = hasErrors
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun getGradleErrors(project: Project): String {
            return RunContentManager.getInstance(project).allDescriptors
                .filter { it.processHandler?.exitCode != 0 }
                .mapNotNull { getErrorFromDescriptor(it) }
                .joinToString("\n")
        }

        private fun getErrorFromDescriptor(descriptor: RunContentDescriptor): String? {
            val content = descriptor.executionConsole?.component?.toString() ?: return null
            // Extract relevant error message from Gradle output
            return content.lines()
                .dropWhile { !it.contains("FAILURE:") }
                .takeWhile { it.isNotBlank() }
                .joinToString("\n")
        }

        fun fixErrorPrompt(errorMessage: String) = "Fix the Gradle build error:\n$errorMessage"

        fun createCommandData(
            project: Project,
            message: String,
            useYesFlag: Boolean,
            isShellMode: Boolean
        ): CommandData {
            val settings = de.andrena.codingaider.settings.AiderSettings.getInstance()
            return CommandData(
                message = message,
                useYesFlag = useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = listOf(FileData("build.gradle.kts", false)),
                isShellMode = isShellMode,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: ""
            )
        }
    }
}

class FixGradleErrorAction : BaseFixGradleErrorAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        fixGradleError(project)
    }

    companion object {
        fun fixGradleError(project: Project) {
            val errorMessage = getGradleErrors(project)
            val commandData = createCommandData(project, fixErrorPrompt(errorMessage), true, false)
            de.andrena.codingaider.executors.api.IDEBasedExecutor(project, commandData).execute()
        }
    }
}

class FixGradleErrorInteractive : BaseFixGradleErrorAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showDialog(project)
    }

    private fun showDialog(project: Project) {
        val errorMessage = getGradleErrors(project)
        val dialog = de.andrena.codingaider.inputdialog.AiderInputDialog(
            project,
            listOf(FileData("build.gradle.kts", false)),
            fixErrorPrompt(errorMessage)
        )

        if (dialog.showAndGet()) {
            val commandData = createCommandData(
                project,
                dialog.getInputText(),
                dialog.isYesFlagChecked(),
                dialog.isShellMode()
            ).copy(
                llm = dialog.getLlm(),
                additionalArgs = dialog.getAdditionalArgs(),
                files = dialog.getAllFiles()
            )

            AiderAction.executeAiderActionWithCommandData(project, commandData)
        }
    }
}
