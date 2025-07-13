package de.andrena.codingaider.toolwindow.runningcommands

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.actions.ide.ShowLastCommandResultAction
import de.andrena.codingaider.services.RunningCommandService
import de.andrena.codingaider.utils.GitUtils
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JOptionPane

class RunningCommandsPanel(private val project: Project) {
    private val runningCommandsListModel = project.service<RunningCommandService>().getRunningCommandsListModel()
    private val runningCommandsList = JBList(runningCommandsListModel)
    fun getContent(): JComponent {
        return panel {
            row {
                val toolbar = ActionManager.getInstance().createActionToolbar(
                    "RunningCommandsToolbar",
                    DefaultActionGroup().apply {
                        add(object : AnAction(
                            "Show Last Command Result",
                            "Show the result of the last command",
                            AllIcons.Actions.Show
                        ) {
                            override fun actionPerformed(e: AnActionEvent) {
                                ShowLastCommandResultAction().showLastCommandFor(project)
                            }
                        })
                        add(object : AnAction(
                            "Create Plan from Last Command",
                            "Create a structured plan from the last command's output and context",
                            AllIcons.Actions.RunAll
                        ) {
                            init {
                                templatePresentation.putClientProperty(
                                    "ActionButton.smallVariant",
                                    true
                                )
                                templatePresentation.icon = AllIcons.Actions.RunAll
                                templatePresentation.disabledIcon = AllIcons.Actions.RunAll
                            }

                            override fun getActionUpdateThread() = ActionUpdateThread.BGT
                            override fun update(e: AnActionEvent) {
                                val hasCompletedCommand = project.service<RunningCommandService>().hasCompletedCommand()
                                e.presentation.isEnabled = hasCompletedCommand
                                e.presentation.text = "Create Plan from Last Command"
                                e.presentation.description = if (hasCompletedCommand) {
                                    "Convert last command into structured plan"
                                } else {
                                    "No completed command available to create plan from"
                                }
                            }

                            override fun actionPerformed(e: AnActionEvent) {
                                try {
                                    project.service<RunningCommandService>().createPlanFromLastCommand(project)
                                } catch (ex: Exception) {
                                    JOptionPane.showMessageDialog(
                                        null,
                                        "Failed to create plan: ${ex.message}",
                                        "Plan Creation Error",
                                        JOptionPane.ERROR_MESSAGE
                                    )
                                }
                            }
                        })
                        add(object : AnAction(
                            "Show Last Aider Diff",
                            "Show the git diff for the last changes done by Aider",
                            AllIcons.Actions.Diff
                        ) {
                            override fun getActionUpdateThread() = ActionUpdateThread.BGT
                            override fun update(e: AnActionEvent) {
                                val hashes = project.service<RunningCommandService>().getLastAiderCommitHashes()
                                e.presentation.isEnabled =
                                    hashes != null && hashes.first != null && hashes.second != null
                                e.presentation.text = "Show Last Aider Diff"
                                e.presentation.description = if (e.presentation.isEnabled) {
                                    "Show the git diff for the last Aider command"
                                } else {
                                    "No Aider command with commit hashes available"
                                }
                            }

                            override fun actionPerformed(e: AnActionEvent) {
                                val hashes = project.service<RunningCommandService>().getLastAiderCommitHashes()
                                if (hashes != null && hashes.first != null && hashes.second != null) {
                                    GitUtils.openGitComparisonTool(project, hashes.first!!) {}
                                } else {
                                    JOptionPane.showMessageDialog(
                                        null,
                                        "No Aider command with commit hashes available.",
                                        "No Diff Available",
                                        JOptionPane.INFORMATION_MESSAGE
                                    )
                                }
                            }
                        })
                    },
                    true
                )
                toolbar.targetComponent = runningCommandsList
                cell(Wrapper(toolbar.component))
            }
            row {
                scrollCell(runningCommandsList)
                    .align(Align.FILL)
                    .resizableColumn()
            }
        }
    }
}
