package de.andrena.codingaider.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.AiderOutputTab
import de.andrena.codingaider.outputview.AiderOutputToolWindowContent
import de.andrena.codingaider.outputview.CodingAiderOutputPresentation
import de.andrena.codingaider.toolwindow.CONTENT_MANAGER_KEY

@Service(Service.Level.PROJECT)
class AiderOutputService(private val project: Project) {
    
    private var toolWindowContent: AiderOutputToolWindowContent? = null
    
    fun createOutput(
        initialTitle: String,
        initialText: String,
        onAbort: Abortable?,
        commandData: CommandData?
    ): CodingAiderOutputPresentation { // Returns AiderOutputTab
        return createToolWindowTab(initialTitle, initialText, onAbort, commandData)
    }
    
    private fun createToolWindowTab(
        initialTitle: String,
        initialText: String,
        onAbort: Abortable?,
        commandData: CommandData?
    ): AiderOutputTab {
        // Get or create tool window content manager
        val contentManager = project.getUserData(CONTENT_MANAGER_KEY)
            ?: run {
                // Tool window not initialized yet, force initialization
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aider Output")
                    ?: throw IllegalStateException("Aider Output tool window not found")
                
                // Show the tool window to trigger initialization
                toolWindow.show()
                
                // Get the content manager after showing
                project.getUserData(CONTENT_MANAGER_KEY)
                    ?: toolWindow.contentManager // Fallback to direct access
            }
        
        if (toolWindowContent == null) {
            toolWindowContent = AiderOutputToolWindowContent(project, contentManager)
        }
        
        val tab = toolWindowContent!!.createTab(initialTitle, initialText, onAbort, commandData)
        
        // Show and focus the tool window
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aider Output")
        toolWindow?.show()
        toolWindow?.activate(null)
        
        return tab
    }
    
    
    fun updateProgress(output: Any, message: String, title: String) {
        when (output) {
            is AiderOutputTab -> toolWindowContent?.updateTabProgress(output, message, title)
        }
    }
    
    fun setProcessFinished(output: Any) {
        when (output) {
            is AiderOutputTab -> toolWindowContent?.setTabFinished(output)
        }
    }
    
    
    fun focus(output: Any) {
        when (output) {
            is AiderOutputTab -> {
                // For tool window tabs, show and focus the tool window
                ApplicationManager.getApplication().invokeLater {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aider Output")
                    toolWindow?.show()
                    toolWindow?.activate(null)
                }
            }
        }
    }
    
    companion object {
        fun getInstance(project: Project): AiderOutputService =
            project.getService(AiderOutputService::class.java)
    }
}
