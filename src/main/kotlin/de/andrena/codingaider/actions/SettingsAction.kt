package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

class SettingsAction : AnAction("Open Settings", "Open Aider Settings", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Aider Settings")
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
