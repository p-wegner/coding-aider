package de.andrena.codingaider.outputview

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AiderOutputToolWindow : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true
    
    override fun isDoNotActivateOnStart() = false

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setToHideOnEmptyContent(false)
        toolWindow.isAutoHide = false
        
        // The content manager will be used by AiderOutputToolWindowContent
        // We don't need to add any initial content here as tabs will be created dynamically
        
        // Store the content manager in the project for access by executors
        project.putUserData(CONTENT_MANAGER_KEY, toolWindow.contentManager)
    }
    
    companion object {
        val CONTENT_MANAGER_KEY = com.intellij.openapi.util.Key.create<com.intellij.ui.content.ContentManager>("AIDER_OUTPUT_CONTENT_MANAGER")
    }
}
