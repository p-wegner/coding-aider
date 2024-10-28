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
        fun getGradleErrors(project: Project): String {
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

            // TODO: Clean up
            val content: String? = when (console) {
                is com.intellij.execution.impl.ConsoleViewImpl -> {
                    console.text
                }
                is BuildView -> {
                    val view = (console.component as BuildView).getView(
                        BuildTreeConsoleView::class.java.getName(),
                        BuildTreeConsoleView::class.java
                    )
                    if (view == null) {
                        val view2 = console.getView("consoleView") as? SMTRunnerConsoleView

                        val testsMapFromConsoleView = view2?.getTestsMapFromConsoleView()
                        val stackTraceAndLocations =
                            (testsMapFromConsoleView?.entries?.map({(it.value as?  SMTestProxy)?.getStackTraceAndLocation()}))
                                ?.filter{it?.first != null && it.second != null}
                        if (stackTraceAndLocations != null) {
                            val (locationUrl, stacktrace) = stackTraceAndLocations.firstOrNull() ?: return null
                            return "Location: $locationUrl\nStacktrace: ${stacktrace?.normalizeLineSeparators()}"
                        }
                    }
                    val entries = (view as? BuildTreeConsoleView)?.getNodesMapFromConsoleView()?.entries
                    entries?.joinToString("\n") { it.value.toString() }
//                    (console.consoleView as? com.intellij.execution.impl.ConsoleViewImpl)?.text
                }

                else -> console.component?.toString()
            } ?: return null

            if (content == null) {
                return null
            }

            return GradleErrorProcessor.extractError(content)
        }

        private object GradleErrorProcessor {

            fun extractError(content: String): String = content
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

private fun String.normalizeLineSeparators(): String = this.replace("\r\n", "\n")

private fun SMTestProxy.getStackTraceAndLocation(): Pair<String?, String?> {
    return this.let {
        val locationUrl = it.getLocationUrl()
        val stacktrace = it.getStacktrace()
        return locationUrl to stacktrace
    }
}

class FixGradleErrorAction : BaseFixGradleErrorAction() {
    override fun getTemplateText(): String = "Quick Fix Gradle Error"
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

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix Gradle error with Aider"
        override fun getText(): String = "Quick fix Gradle error with Aider"

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

class FixGradleErrorInteractive : BaseFixGradleErrorAction() {
    override fun getTemplateText(): String = "Fix Gradle Error (Interactive)"
    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix Gradle error with Aider"
        override fun getText(): String = "Fix Gradle error with Aider (Interactive)"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            return project != null && hasGradleErrors(project)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            FixGradleErrorInteractive().showDialog(project)
        }
    }

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
fun BuildTreeConsoleView.getNodesMapFromConsoleView(): Map<*, *>? {
    try {
        val nodesMapField = BuildTreeConsoleView::class.java.getDeclaredField("nodesMap")
        nodesMapField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return nodesMapField.get(this) as? Map<*, *>
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
fun SMTRunnerConsoleView.getTestsMapFromConsoleView(): Map<*, *>? {
    try {
        val nodesMapField = this::class.java.getDeclaredField("testsMap")
        nodesMapField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return nodesMapField.get(this) as? Map<*, *>
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}