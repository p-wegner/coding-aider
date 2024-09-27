package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.IDEBasedExecutor
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance

class CommitAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)

        if (project != null && !files.isNullOrEmpty()) {
            val settings = getInstance()
            val commandData = CommandData(
                message = "/commit",
                useYesFlag = true,
                llm = settings.llm,
                additionalArgs = "",
                files = emptyList(),
                isShellMode = false,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                projectPath = project.basePath ?: "",
                structuredMode = false
            )
            IDEBasedExecutor(project, commandData).execute()
        }
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
