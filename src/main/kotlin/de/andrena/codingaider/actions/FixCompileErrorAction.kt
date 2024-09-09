package de.andrena.codingaider.actions

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
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
            return DaemonCodeAnalyzerImpl.getHighlights(
                document,
                HighlightInfoType.ERROR.getSeverity(psiFile),
                project
            )
        }

        fun hasCompileErrors(project: Project, psiFile: PsiFile): Boolean {
            return getCompileErrors(project, psiFile).isNotEmpty()
        }

        fun getErrorMessage(project: Project, psiFile: PsiFile): String {
            val errors = getCompileErrors(project, psiFile)
            return errors.joinToString("\n") { it.description }
        }

        fun createCommandData(
            project: Project,
            psiFile: PsiFile,
            message: String,
            useYesFlag: Boolean,
            isShellMode: Boolean
        ): CommandData {
            return CommandData(
                message = message,
                useYesFlag = useYesFlag,
                llm = AiderSettings.getInstance(project).llm,
                additionalArgs = AiderSettings.getInstance(project).additionalArgs,
                files = listOf(FileData(psiFile.virtualFile.path, false)),
                isShellMode = isShellMode,
                lintCmd = AiderSettings.getInstance(project).lintCmd
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
                createCommandData(project, psiFile, "fix the compile error in this file: $errorMessage", true, false)
            IDEBasedExecutor(project, commandData).execute()
        }
    }

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix compile error with Aider"
        override fun getText(): String = "Quick fix compile error with Aider"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            return hasCompileErrors(project, element.containingFile)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            fixCompileError(project, element.containingFile)
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
            "fix the compile error in this file: $errorMessage"
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

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean =
            hasCompileErrors(project, element.containingFile)

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) =
            FixCompileErrorInteractive().showDialog(project, element.containingFile)
    }
}
