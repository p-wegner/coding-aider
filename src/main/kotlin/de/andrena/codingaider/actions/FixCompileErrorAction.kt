package de.andrena.codingaider.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.psi.PsiDocumentManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings

class FixCompileErrorAction : PsiElementBaseIntentionAction(), IntentionAction {
    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val file = element.containingFile
        return file != null && hasCompileErrors(project, file)
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val file = element.containingFile
        if (file != null) {
            val errors = getCompileErrors(project, file)
            val errorMessage = errors.joinToString("\n") { it.description }

            val commandData = CommandData(
                message = "fix the compile error in this file: $errorMessage",
                useYesFlag = true,
                llm = AiderSettings.getInstance(project).llm,
                additionalArgs = AiderSettings.getInstance(project).additionalArgs,
                files = listOf(FileData(file.virtualFile.path, false)),
                isShellMode = false,
                lintCmd = AiderSettings.getInstance(project).lintCmd
            )
            IDEBasedExecutor(project, commandData).execute()
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = "Fix with Aider"

    override fun getText(): String = "Fix compile error with Aider"

    private fun getCompileErrors(project: Project, psiFile: PsiFile): List<HighlightInfo> {
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return emptyList()
        return DaemonCodeAnalyzerImpl.getHighlights(
            document,
            HighlightInfoType.ERROR.getSeverity(psiFile),
            project
        )
    }

    private fun hasCompileErrors(project: Project, psiFile: PsiFile): Boolean {
        return getCompileErrors(project, psiFile).isNotEmpty()
    }
}
