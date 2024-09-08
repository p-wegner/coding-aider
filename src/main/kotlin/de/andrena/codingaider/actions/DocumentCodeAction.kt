package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings
import java.io.File

class DocumentCodeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        documentCode(project, psiFile)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = project != null && psiFile != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun documentCode(project: Project, psiFile: PsiFile) {
            val filename = Messages.showInputDialog(
                project,
                "Enter the filename to store the documentation:",
                "Document Code",
                Messages.getQuestionIcon()
            ) ?: return

            val fullPath = File(project.basePath, filename).absolutePath

            val files = emptyList<String>()
            val commandData = CommandData(
                message = "Generate a markdown documentation for the code in the provided files $files . If there are exceptional implementation details, mention them. Store the results in $fullPath.",
                useYesFlag = true,
                llm = AiderSettings.getInstance(project).llm,
                additionalArgs = AiderSettings.getInstance(project).additionalArgs,
                files = listOf(FileData(psiFile.virtualFile.path, false), FileData(fullPath, true)),
                isShellMode = false,
                lintCmd = AiderSettings.getInstance(project).lintCmd
            )
            IDEBasedExecutor(project, commandData).execute()
        }
    }
}
