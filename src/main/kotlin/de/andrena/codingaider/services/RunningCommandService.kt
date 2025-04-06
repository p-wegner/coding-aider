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
            // TODO: Implement plan creation from last command
            JOptionPane.showMessageDialog(
                null,
                "Plan creation from previous command feature is coming soon!",
                "Create Plan",
                JOptionPane.INFORMATION_MESSAGE
            )
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
