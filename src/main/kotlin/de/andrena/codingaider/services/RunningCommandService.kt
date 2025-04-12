package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.MarkdownDialog
import javax.swing.DefaultListModel
import javax.swing.JOptionPane

@Service(Service.Level.PROJECT)
class RunningCommandService {
    private val runningCommandsListModel = DefaultListModel<MarkdownDialog>()
    private var lastCompletedCommand: CommandData? = null
    private var lastCommandOutput: String? = null

    fun addRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.addElement(dialog)
    }

    fun removeRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.removeElement(dialog)
    }

    fun getRunningCommandsListModel(): DefaultListModel<MarkdownDialog> = runningCommandsListModel
    
    fun storeCompletedCommand(commandData: CommandData?, output: String?) {
        lastCompletedCommand = commandData
        lastCommandOutput = output
    }
    
    fun createPlanFromLastCommand(project: Project) {
        if (lastCompletedCommand == null || lastCommandOutput == null) {
            JOptionPane.showMessageDialog(
                null,
                "No completed command available to create a plan from.",
                "Create Plan",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }
        
        try {
            val command = lastCompletedCommand!!
            val output = lastCommandOutput!!
            
            // Create structured mode command data with all relevant files
            val planCommand = command.copy(
                message = "Create plan from previous command:\n$output",
                aiderMode = AiderMode.STRUCTURED,
                files = command.files + project.service<AiderPlanService>()
                    .getAiderPlans()
                    .flatMap { it.allFiles }
            )
            
            // Execute the plan creation command in structured mode
            val executor = IDEBasedExecutor(project, planCommand)
            executor.execute()
            
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Error during plan creation: ${e.message}",
                "Plan Creation Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    fun hasCompletedCommand(): Boolean = lastCompletedCommand != null
}
