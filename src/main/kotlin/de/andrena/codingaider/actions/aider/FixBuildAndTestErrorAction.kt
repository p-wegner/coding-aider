package de.andrena.codingaider.actions.aider

import com.intellij.build.BuildTreeConsoleView
import com.intellij.build.BuildView
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import de.andrena.codingaider.actions.aider.FixBuildAndTestErrorActionGroup.Companion.hasGradleErrors
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.utils.ReflectionUtils

class FixBuildAndTestErrorActionGroup : ActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> =
        arrayOf(FixBuildAndTestErrorAction(), FixBuildAndTestErrorInteractive())

    override fun update(e: AnActionEvent) {
        val project = e.project
        val hasErrors = project != null && hasGradleErrors(project)
        e.presentation.isEnabledAndVisible = hasErrors
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun hasGradleErrors(project: Project): Boolean =
            RunContentManager.getInstance(project).allDescriptors.any { it.processHandler != null && it.processHandler?.exitCode?.let { it != 0 } ?: false }
    }
}

abstract class BaseFixBuildAndTestErrorAction : AnAction() {
    abstract override fun getTemplateText(): String

    init {
        templatePresentation.text = getTemplateText()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val hasErrors = project != null && hasGradleErrors(project)
        e.presentation.isEnabledAndVisible = hasErrors
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun getErrors(project: Project): String {
            return RunContentManager.getInstance(project).allDescriptors
                .filter { it.processHandler?.exitCode != 0 }
                .mapNotNull { getErrorFromDescriptor(it) }
                .joinToString("\n")
        }

        private fun getErrorFromDescriptor(descriptor: RunContentDescriptor): String? {
            val console = descriptor.executionConsole
            if (console == null || descriptor.processHandler?.exitCode == 0) {
                return null
            }
            val content: String = extractConsoleContent(console) ?: return null
            return GradleErrorProcessor.extractError(content)
        }

        private object GradleErrorProcessor {
            fun extractError(content: String): String = content
        }

        private fun extractConsoleContent(console: com.intellij.execution.ui.ExecutionConsole): String? {
            return when (console) {
                is com.intellij.execution.impl.ConsoleViewImpl -> {
                    console.text
                }

                is BuildView -> extractBuildViewContent(console)
                else -> console.component?.toString()
            }
        }

        private fun extractBuildViewContent(buildView: BuildView): String? {
            // Try to get build tree view first
            val buildTreeView: BuildTreeConsoleView? = (buildView.component as BuildView).getView(
                BuildTreeConsoleView::class.java.getName(),
                BuildTreeConsoleView::class.java
            )

            if (buildTreeView != null) {
                val entries = ReflectionUtils.getNodesMapFromBuildView(buildTreeView)?.entries
                return entries?.joinToString("\n") { it.value.toString() }
            }

            // Fall back to test runner view
            val testRunnerView = buildView.getView("consoleView") as? SMTRunnerConsoleView ?: return null
            val testsMap = ReflectionUtils.getTestsMapFromConsoleView(testRunnerView)

            val stackTraceAndLocations = testsMap?.entries?.mapNotNull { entry ->
                (entry.value as? SMTestProxy)?.getStackTraceAndLocation()
            }?.filter { it.first != null && it.second != null }

            return stackTraceAndLocations?.firstNotNullOfOrNull { (locationUrl, stacktrace) ->
                "Location: $locationUrl\nStacktrace: ${stacktrace?.normalizeLineSeparators()}"
            }
        }

        fun fixErrorPrompt(errorMessage: String) = "Fix this error:\n$errorMessage"

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
                files = listOf(),
                isShellMode = isShellMode,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: ""
            )
        }
    }
}

private fun String.normalizeLineSeparators(): String = this.replace("\r\n", "\n")

private fun SMTestProxy.getStackTraceAndLocation(): Pair<String?, String?> = this.let {
    val locationUrl = it.locationUrl
    val stacktrace = it.stacktrace
    return locationUrl to stacktrace
}

class FixBuildAndTestErrorAction : BaseFixBuildAndTestErrorAction() {
    override fun getTemplateText(): String = "Quick Fix Error"
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        fixGradleError(project)
    }

    companion object {
        fun fixGradleError(project: Project) {
            val errorMessage = getErrors(project)
            val commandData = createCommandData(project, fixErrorPrompt(errorMessage), true, false)
            de.andrena.codingaider.executors.api.IDEBasedExecutor(project, commandData).execute()
        }
    }

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix error with Aider"
        override fun getText(): String = "Quick fix error with Aider"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            if (project == null) return false

            // Check if we're in a console view
            val consoleView = element.containingFile?.virtualFile?.let {
                RunContentManager.getInstance(project).allDescriptors
                    .find { descriptor -> descriptor.executionConsole?.component?.toString() == it.path }
                    ?.executionConsole
            }

            return consoleView != null && hasGradleErrors(project)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            fixGradleError(project)
        }
    }
}

class FixBuildAndTestErrorInteractive : BaseFixBuildAndTestErrorAction() {
    override fun getTemplateText(): String = "Fix Error (Interactive)"
    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix error with Aider"
        override fun getText(): String = "Fix error with Aider (Interactive)"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            return project != null && hasGradleErrors(project)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            FixBuildAndTestErrorInteractive().showDialog(project)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showDialog(project)
    }

    private fun showDialog(project: Project) {
        val errorMessage = getErrors(project)
        val dialog = AiderInputDialog(
            project,
            listOf(),
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
