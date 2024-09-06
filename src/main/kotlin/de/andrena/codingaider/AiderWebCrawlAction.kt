package de.andrena.codingaider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.IDEBasedExecutor

class AiderWebCrawlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url =
            Messages.showInputDialog(project, "Enter URL to crawl:", "Aider Web Crawl", Messages.getQuestionIcon())
        if (!url.isNullOrEmpty()) {
            val commandData = CommandData(
                message = " /web $url",
                useYesFlag = true,
                llm = "4o",
                additionalArgs = "",
                files = emptyList(),
                isShellMode = false,
                lintCmd = ""
            )
            IDEBasedExecutor(project, commandData).execute()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
