package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AiderToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val aiderTerminalPanel = AiderTerminalPanel(project)
        val content = ContentFactory.getInstance().createContent(aiderTerminalPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}