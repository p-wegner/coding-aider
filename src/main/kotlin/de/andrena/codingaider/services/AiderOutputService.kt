package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.AiderOutputTab
import de.andrena.codingaider.outputview.AiderOutputToolWindow
import de.andrena.codingaider.outputview.AiderOutputToolWindowContent
import de.andrena.codingaider.outputview.MarkdownDialog
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class AiderOutputService(private val project: Project) {
    
    private var toolWindowContent: AiderOutputToolWindowContent? = null
    
    fun createOutput(
        initialTitle: String,
        initialText: String,
        onAbort: Abortable?,
        displayString: String?,
        commandData: CommandData?
    ): Any { // Returns either MarkdownDialog or AiderOutputTab
        val settings = AiderSettings.getInstance()
        
        return if (settings.useToolWindowOutput) {
            createToolWindowTab(initialTitle, initialText, onAbort, commandData)
        } else {
            createDialog(initialTitle, initialText, onAbort, displayString, commandData)
        }
    }
    
    private fun createToolWindowTab(
        initialTitle: String,
        initialText: String,
        onAbort: Abortable?,
        commandData: CommandData?
    ): AiderOutputTab {
        // Get or create tool window content manager
        val contentManager = project.getUserData(AiderOutputToolWindow.CONTENT_MANAGER_KEY)
            ?: run {
                // Tool window not initialized yet, force initialization
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aider Output")
                toolWindow?.show()
                project.getUserData(AiderOutputToolWindow.CONTENT_MANAGER_KEY)
            }
            ?: throw IllegalStateException("Could not initialize Aider Output tool window")
        
        if (toolWindowContent == null) {
            toolWindowContent = AiderOutputToolWindowContent(project, contentManager)
        }
        
        val tab = toolWindowContent!!.createTab(initialTitle, initialText, onAbort, commandData)
        
        // Show the tool window
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aider Output")
        toolWindow?.show()
        
        return tab
    }
    
    private fun createDialog(
        initialTitle: String,
        initialText: String,
        onAbort: Abortable?,
        displayString: String?,
        commandData: CommandData?
    ): MarkdownDialog {
        return MarkdownDialog(
            project,
            initialTitle,
            initialText,
            onAbort,
            displayString,
            commandData
        ).apply {
            isVisible = true
            focus()
        }
    }
    
    fun updateProgress(output: Any, message: String, title: String) {
        when (output) {
            is MarkdownDialog -> output.updateProgress(message, title)
            is AiderOutputTab -> toolWindowContent?.updateTabProgress(output, message, title)
        }
    }
    
    fun setProcessFinished(output: Any) {
        when (output) {
            is MarkdownDialog -> output.setProcessFinished()
            is AiderOutputTab -> toolWindowContent?.setTabFinished(output)
        }
    }
    
    fun startAutoCloseTimer(output: Any, delay: Int) {
        when (output) {
            is MarkdownDialog -> output.startAutoCloseTimer(delay)
            is AiderOutputTab -> output.startAutoCloseTimer(delay)
        }
    }
    
    fun focus(output: Any, delay: Long = 100) {
        when (output) {
            is MarkdownDialog -> output.focus(delay)
            is AiderOutputTab -> {
                // For tool window tabs, just ensure the tool window is visible
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aider Output")
                toolWindow?.show()
            }
        }
    }
    
    companion object {
        fun getInstance(project: Project): AiderOutputService =
            project.getService(AiderOutputService::class.java)
    }
}
