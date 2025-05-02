package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.outputview.MarkdownDialog
import javax.swing.DefaultListModel
import javax.swing.JOptionPane

@Service(Service.Level.PROJECT)
class RunningCommandService {
    private val runningCommandsListModel = DefaultListModel<MarkdownDialog>()
    private var lastCompletedCommand: CommandData? = null
    private var lastCommandOutput: String? = null

    // Store commit hashes for last aider command
    private var lastAiderCommitBefore: String? = null
    private var lastAiderCommitAfter: String? = null

    fun addRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.addElement(dialog)
    }

    fun removeRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.removeElement(dialog)
    }

    fun getRunningCommandsListModel(): DefaultListModel<MarkdownDialog> = runningCommandsListModel

    fun storeCompletedCommand(commandData: CommandData?, output: String?, commitBefore: String? = null, commitAfter: String? = null) {
        lastCompletedCommand = commandData
        lastCommandOutput = output
        if (commitBefore != null && commitAfter != null) {
            lastAiderCommitBefore = commitBefore
            lastAiderCommitAfter = commitAfter
        }
    }

    fun getLastAiderCommitHashes(): Pair<String?, String?>? {
        return if (lastAiderCommitBefore != null && lastAiderCommitAfter != null) {
            Pair(lastAiderCommitBefore, lastAiderCommitAfter)
        } else {
            null
        }
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
            val planCreationService = project.service<PostActionPlanCreationService>()
            planCreationService.createPlanFromCommand(lastCompletedCommand!!, lastCommandOutput!!)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Error during plan creation: ${e.message}",
                "Plan Creation Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    fun hasCompletedCommand(): Boolean = lastCompletedCommand != null && lastCommandOutput != null

}

