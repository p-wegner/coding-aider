package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings

class FixCompileErrorAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project != null && file != null) {
            val psiFile: PsiFile? = PsiManager.getInstance(project).findFile(file)
            val errors = getCompileErrors(project, psiFile)
            val errorMessage = errors.joinToString("\n") { it.description }

            val commandData = CommandData(
                message = "fix the compile error in this file: $errorMessage",
                useYesFlag = true,
                llm = AiderSettings.getInstance(project).llm,
                additionalArgs = AiderSettings.getInstance(project).additionalArgs,
                files = listOf(FileData(file.path, false)),
                isShellMode = false,
                lintCmd = AiderSettings.getInstance(project).lintCmd
            )
            IDEBasedExecutor(project, commandData).execute()
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val file: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val psiFile: PsiFile? = if (project != null && file != null) PsiManager.getInstance(project).findFile(file) else null
        
        e.presentation.isEnabledAndVisible = project != null && file != null && hasCompileErrors(project, psiFile)
    }

    private fun getCompileErrors(project: Project, psiFile: PsiFile?): List<HighlightInfo> {
        if (psiFile == null) return emptyList()
        return DaemonCodeAnalyzerImpl.getHighlights(psiFile.document, HighlightInfo.SEVERITY.ERROR, project)
    }

    private fun hasCompileErrors(project: Project?, psiFile: PsiFile?): Boolean {
        if (project == null || psiFile == null) return false
        return getCompileErrors(project, psiFile).isNotEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
