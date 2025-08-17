package de.andrena.codingaider.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentManager

class AiderOutputToolWindow : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = false

    override fun isDumbAware(): Boolean = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setToHideOnEmptyContent(true)
        toolWindow.isAutoHide = true
        project.putUserData(CONTENT_MANAGER_KEY, toolWindow.contentManager)
    }

}

val CONTENT_MANAGER_KEY = Key.create<ContentManager>("AIDER_OUTPUT_CONTENT_MANAGER")