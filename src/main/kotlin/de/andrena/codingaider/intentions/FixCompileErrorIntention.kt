package de.andrena.codingaider.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.andrena.codingaider.actions.FixCompileErrorAction

class FixCompileErrorIntention : PsiElementBaseIntentionAction(), IntentionAction {
    override fun getText(): String = "Fix compile error with Aider"

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return FixCompileErrorAction.hasCompileErrors(project, element.containingFile)
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        FixCompileErrorAction.fixCompileError(project, element.containingFile)
    }
}
