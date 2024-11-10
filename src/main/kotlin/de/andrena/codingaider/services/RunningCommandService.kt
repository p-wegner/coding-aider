package de.andrena.codingaider.services

import de.andrena.codingaider.outputview.MarkdownDialog
import javax.swing.DefaultListModel

class RunningCommandService {
    private val runningCommandsListModel = DefaultListModel<MarkdownDialog>()

    fun addRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.addElement(dialog)
    }

    fun removeRunningCommand(dialog: MarkdownDialog) {
        runningCommandsListModel.removeElement(dialog)
    }

    fun getRunningCommandsListModel(): DefaultListModel<MarkdownDialog> {
        return runningCommandsListModel
    }
}
