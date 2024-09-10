package de.andrena.codingaider.actions

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.settings.AiderSettings

abstract class BaseFixCompileErrorAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = project != null && psiFile != null && hasCompileErrors(project, psiFile)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        private fun getCompileErrors(project: Project, psiFile: PsiFile): List<HighlightInfo> {
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return emptyList()
            return DocumentMarkupModel.forDocument(document, project, true).allHighlighters
                .filter { (it.errorStripeTooltip as? HighlightInfo)?.severity == HighlightSeverity.ERROR}
                .map { it.errorStripeTooltip as HighlightInfo }
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
            psiFile: PsiFile,
            message: String,
            useYesFlag: Boolean,
            isShellMode: Boolean
        ): CommandData {
            val settings = AiderSettings.getInstance(project)
            val filePath = psiFile.virtualFile?.path
            val files = filePath?.let { listOf(FileData(it, false)) } ?: emptyList()
            return CommandData(
                message = message,
                useYesFlag = useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = files,
                isShellMode = isShellMode,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat
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

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun fixCompileError(project: Project, psiFile: PsiFile) {
            val errorMessage = getErrorMessage(project, psiFile)
            val commandData =
                createCommandData(project, psiFile, fixErrorPrompt(errorMessage), true, false)
            IDEBasedExecutor(project, commandData).execute()
        }


    }

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix compile error with Aider"
        override fun getText(): String = "Quick fix compile error with Aider"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            return editor != null && hasCompileErrors(project, element.containingFile) && isExplicitlyInvoked()
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            fixCompileError(project, element.containingFile)
        }

        private fun isExplicitlyInvoked(): Boolean {
            return com.intellij.openapi.actionSystem.ActionPlaces.isPopupPlace(com.intellij.openapi.actionSystem.ActionPlaces.getActionPlace())
        }
    }

}

class FixCompileErrorInteractive : BaseFixCompileErrorAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        showDialog(project, psiFile)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun showDialog(project: Project, psiFile: PsiFile) {
        val errorMessage = getErrorMessage(project, psiFile)
        val dialog = AiderInputDialog(
            project,
            listOf(FileData(psiFile.virtualFile.path, false)),
            fixErrorPrompt(errorMessage)
        )

        if (dialog.showAndGet()) {
            val commandData = createCommandData(
                project,
                psiFile,
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

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix compile error with Aider"
        override fun getText(): String = "Fix compile error with Aider (Interactive)"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            return editor != null && hasCompileErrors(project, element.containingFile) && isExplicitlyInvoked()
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            FixCompileErrorInteractive().showDialog(project, element.containingFile)
        }

        private fun isExplicitlyInvoked(): Boolean {
            return com.intellij.openapi.actionSystem.ActionPlaces.isPopupPlace(com.intellij.openapi.actionSystem.ActionPlaces.getActionPlace())
        }
    }
}
