package de.andrena.codingaider.actions.aider

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance


abstract class BaseFixTodoAction : AnAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val hasTodos = project != null && psiFile != null && hasTodos(project, psiFile)
        e.presentation.isEnabledAndVisible = hasTodos
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        private fun getTodos(project: Project, psiFile: PsiFile): List<PsiComment> {
            return PsiTreeUtil.findChildrenOfType(psiFile, PsiComment::class.java)
                .filter { it.text.contains("TODO", ignoreCase = true) }
        }

        fun fixTodoPrompt(todoText: String, psiFile: PsiFile) = "Fix the TODO in ${psiFile.name}:\n$todoText"

        fun hasTodos(project: Project, psiFile: PsiFile): Boolean =
            getTodos(project, psiFile).isNotEmpty()

        fun getTodoText(project: Project, psiFile: PsiFile): String {
            val todos = getTodos(project, psiFile)
            return todos.joinToString("\n") { it.text }
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

class FixTodoAction : BaseFixTodoAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        fixTodo(project, psiFile)
    }

    init {
        templatePresentation.text = "Quick Fix TODO"
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    companion object {
        fun fixTodo(project: Project, psiFile: PsiFile) {
            val todoText = getTodoText(project, psiFile)
            val files = getFiles(psiFile, project)
            val commandData = createCommandData(project, files, fixTodoPrompt(todoText, psiFile), true)
            IDEBasedExecutor(project, commandData).execute()
        }
    }

    class Intention : PsiElementBaseIntentionAction(), IntentionAction {
        override fun getFamilyName(): String = "Fix TODO with Aider"
        override fun getText(): String = "Quick fix TODO with Aider"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            val psiFile = element.containingFile
            return editor != null && psiFile != null && hasTodos(project, psiFile)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            if (element.containingFile.virtualFile == null) return
            fixTodo(project, element.containingFile)
        }
    }
}

class FixTodoInteractive : BaseFixTodoAction() {
    init {
        templatePresentation.text = "Fix TODO (Interactive)"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        showDialogInBGT(project, psiFile)
    }

    fun showDialogInBGT(project: Project, psiFile: PsiFile) {
        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fixing TODO", true) {
                override fun run(indicator: ProgressIndicator) = showDialog(project, psiFile)
            })
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun showDialog(project: Project, psiFile: PsiFile) {
        val todoText = getTodoText(project, psiFile)
        ApplicationManager.getApplication().invokeAndWait {
            getFiles(psiFile, project).let { files ->
                val dialog = AiderInputDialog(
                    project,
                    files,
                    fixTodoPrompt(todoText, psiFile)
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
        override fun getFamilyName(): String = "Fix TODO with Aider"
        override fun getText(): String = "Fix TODO with Aider (Interactive)"

        override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
            val psiFile = element.containingFile
            return editor != null && psiFile != null && hasTodos(project, psiFile)
        }

        override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
            if (element.containingFile.virtualFile == null) return
            FixTodoInteractive().showDialogInBGT(project, element.containingFile)
        }
    }
}
