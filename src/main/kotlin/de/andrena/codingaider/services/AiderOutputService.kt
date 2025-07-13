package de.andrena.codingaider.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.Abortable
import de.andrena.codingaider.outputview.AiderOutputTab
import de.andrena.codingaider.outputview.AiderOutputToolWindow
import de.andrena.codingaider.outputview.AiderOutputToolWindowContent
import de.andrena.codingaider.outputview.CodingAiderOutputPresentation
import de.andrena.codingaider.services.plans.ActivePlanService
import de.andrena.codingaider.services.plans.ContinuePlanService
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
        val contentManager = project.getUserData(AiderOutputToolWindow.CONTENT_MANAGER_KEY)
            ?: run {
                // Tool window not initialized yet, initialize asynchronously
                ApplicationManager.getApplication().invokeLater {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Aider Output")
                    toolWindow?.show()
                }
                // Try to get it again after initialization
                project.getUserData(AiderOutputToolWindow.CONTENT_MANAGER_KEY)
            }
            ?: throw IllegalStateException("Could not initialize Aider Output tool window")
        
        if (toolWindowContent == null) {
            toolWindowContent = AiderOutputToolWindowContent(project, contentManager)
        }
        
        val tab = toolWindowContent!!.createTab(initialTitle, initialText, onAbort, commandData)
        
        // Show and focus the tool window immediately
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
    
    fun startAutoCloseTimer(output: Any, delay: Int) {
        when (output) {
            is AiderOutputTab -> output.startAutoCloseTimer(delay)
        }
    }
    
    fun triggerAutoContinue(commandData: CommandData?) {
        val settings = AiderSettings.getInstance()
        if (!settings.enableAutoPlanContinue || commandData?.structuredMode != true) {
            return
        }
        
        try {
            // Check if there's an active plan and it's not finished
            val activePlan = project.service<ActivePlanService>().getActivePlan()
            if (activePlan != null && !activePlan.isPlanComplete()) {
                val app = ApplicationManager.getApplication()
                app.invokeLater {
                    ReadAction.run<RuntimeException> {
                        project.service<ContinuePlanService>().continuePlan()
                    }
                }
            }
        } catch (e: Exception) {
            com.intellij.openapi.diagnostic.Logger.getInstance(AiderOutputService::class.java)
                .error("Error during auto continue", e)
        }
    }
    
    fun focus(output: Any, delay: Long = 100) {
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
