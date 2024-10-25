package de.andrena.codingaider.actions.aider

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
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
            return GradleErrorProcessor.extractError(content)
        }

        private object GradleErrorProcessor {
            data class GradleError(
                val message: String,
                val severity: ErrorSeverity,
                val location: ErrorLocation?
            )

            enum class ErrorSeverity {
                CRITICAL, // Build failures, compilation errors
                ERROR,    // Runtime errors
                WARNING   // Deprecation warnings, etc
            }

            data class ErrorLocation(
                val file: String,
                val line: Int?,
                val column: Int?
            )

            fun extractError(content: String): String {
                val error = processError(content)
                return formatError(error)
            }

            private fun processError(content: String): GradleError {
                val lines = content.lines()

                // Try to find build failure message first
                val errorSection = lines
                    .dropWhile { !it.contains("FAILURE:") }
                    .takeWhile { !it.contains("* Try:") && it.isNotBlank() }
                    .filter { it.isNotBlank() }

                if (errorSection.isNotEmpty()) {
                    return GradleError(
                        message = errorSection.joinToString("\n"),
                        severity = ErrorSeverity.CRITICAL,
                        location = extractErrorLocation(errorSection)
                    )
                }

                // Look for other error patterns
                val errorLine = lines.firstOrNull { line ->
                    line.contains("error:", ignoreCase = true) ||
                            line.contains("failed", ignoreCase = true)
                }

                return if (errorLine != null) {
                    GradleError(
                        message = errorLine,
                        severity = determineSeverity(errorLine),
                        location = extractErrorLocation(listOf(errorLine))
                    )
                } else {
                    GradleError(
                        message = "Unknown Gradle error",
                        severity = ErrorSeverity.ERROR,
                        location = null
                    )
                }
            }

            private fun determineSeverity(errorLine: String): ErrorSeverity = when {
                errorLine.contains("FAILURE:", ignoreCase = true) -> ErrorSeverity.CRITICAL
                errorLine.contains("error:", ignoreCase = true) -> ErrorSeverity.ERROR
                else -> ErrorSeverity.WARNING
            }

            private fun extractErrorLocation(errorLines: List<String>): ErrorLocation? {
                // Match patterns like: /path/to/file.kt:line:column
                val locationPattern = Regex("""([\w/.-]+\.\w+):(\d+)(?::(\d+))?""")

                for (line in errorLines) {
                    locationPattern.find(line)?.let { match ->
                        return ErrorLocation(
                            file = match.groupValues[1],
                            line = match.groupValues[2].toIntOrNull(),
                            column = match.groupValues[3].toIntOrNull()
                        )
                    }
                }
                return null
            }

            private fun formatError(error: GradleError): String {
                val locationStr = error.location?.let { loc ->
                    "\nLocation: ${loc.file}" +
                            (loc.line?.let { ":$it" } ?: "") +
                            (loc.column?.let { ":$it" } ?: "")
                } ?: ""

                return "${error.severity}: ${error.message}$locationStr"
            }
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

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix Gradle error with Aider"
        override fun getText(): String = "Quick fix Gradle error with Aider"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            return project != null && hasGradleErrors(project)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            fixGradleError(project)
        }
    }
}

class FixGradleErrorInteractive : BaseFixGradleErrorAction() {
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
