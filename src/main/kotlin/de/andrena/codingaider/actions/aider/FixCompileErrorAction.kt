package de.andrena.codingaider.actions.aider

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance

class FixCompileErrorActionGroup : DefaultActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            FixCompileErrorAction(),
            FixCompileErrorInteractive()
        )
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val hasErrors =
            project != null && psiFile != null && BaseFixCompileErrorAction.hasCompileErrors(project, psiFile)
        e.presentation.isEnabledAndVisible = hasErrors
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

abstract class BaseFixCompileErrorAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val hasErrors = project != null && psiFile != null && hasCompileErrors(project, psiFile)
        e.presentation.isEnabledAndVisible = hasErrors
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        private fun getCompileErrors(project: Project, psiFile: PsiFile): List<HighlightInfo> {
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return emptyList()
            val result = DocumentMarkupModel.forDocument(document, project, true).allHighlighters
                .filter { (it.errorStripeTooltip as? HighlightInfo)?.severity == HighlightSeverity.ERROR }
                .map { it.errorStripeTooltip as HighlightInfo }
            return result
        }

        fun fixErrorPrompt(errorMessage: String) = "Fix the compile error in this file:\n$errorMessage"

        fun hasCompileErrors(project: Project, psiFile: PsiFile): Boolean =
            getCompileErrors(project, psiFile).isNotEmpty()

        fun getErrorMessage(project: Project, psiFile: PsiFile): String {
            val errors = getCompileErrors(project, psiFile)
            return errors.joinToString("\n") { it.description ?: "Unknown error" }
        }

        fun createCommandData(
            project: Project,
            files: List<FileData>,
            message: String,
            useYesFlag: Boolean
        ): CommandData {
            val settings = getInstance()
            return CommandData(
                message = message,
                useYesFlag = useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = files,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                sidecarMode = settings.useSidecarMode
            )
        }
    }
}

class FixCompileErrorAction : BaseFixCompileErrorAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        fixCompileError(project, psiFile)
    }

    init {
        templatePresentation.text = "Quick Fix Compile Error"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun fixCompileError(project: Project, psiFile: PsiFile) {
            val errorMessage = getErrorMessage(project, psiFile)
            val files = getFiles(psiFile, project)
            val commandData = createCommandData(project, files, fixErrorPrompt(errorMessage), true)
            IDEBasedExecutor(project, commandData).execute()
        }


    }

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix compile error with Aider"
        override fun getText(): String = "Quick fix compile error with Aider"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            val psiFile = element.containingFile
            return editor != null && psiFile != null && hasCompileErrors(project, psiFile)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            // workaround to prevent triggering when focussed
            if (element.containingFile.virtualFile == null) return
            fixCompileError(project, element.containingFile)
        }
    }

}

class FixCompileErrorInteractive : BaseFixCompileErrorAction() {
    init {
        templatePresentation.text = "Fix Compile Error (Interactive)"
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        showDialogInBGT(project, psiFile)
    }

    fun showDialogInBGT(project: Project, psiFile: PsiFile) {
        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fixing Compile Error", true) {
                override fun run(indicator: ProgressIndicator) = showDialog(project, psiFile)
            })
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun showDialog(project: Project, psiFile: PsiFile) {
        val errorMessage = getErrorMessage(project, psiFile)
        ApplicationManager.getApplication().invokeAndWait {
            getFiles(psiFile, project).let { files ->
                val dialog = AiderInputDialog(
                    project,
                    files,
                    fixErrorPrompt(errorMessage)
                )

                if (dialog.showAndGet()) {
                    val commandData = createCommandData(
                        project,
                        files,
                        dialog.getInputText(),
                        dialog.isYesFlagChecked()
                    ).copy(
                        llm = dialog.getLlm().name,
                        additionalArgs = dialog.getAdditionalArgs(),
                        files = dialog.getAllFiles()
                    )

                    AiderAction.executeAiderActionWithCommandData(project, commandData)
                }
            }
        }
    }

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix compile error with Aider"
        override fun getText(): String = "Fix compile error with Aider (Interactive)"
        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            val psiFile = element.containingFile
            return editor != null && psiFile != null && hasCompileErrors(project, psiFile)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            // workaround to prevent triggering when focussed
            if (element.containingFile.virtualFile == null) return
            FixCompileErrorInteractive().showDialogInBGT(project, element.containingFile)
        }

    }

}

fun getFiles(psiFile: PsiFile, project: Project): List<FileData> {
    val elements = psiFile.virtualFile?.let { arrayOf(it) } ?: emptyArray()
    val files = project.service<FileDataCollectionService>().collectAllFiles(elements)
    return files
}
