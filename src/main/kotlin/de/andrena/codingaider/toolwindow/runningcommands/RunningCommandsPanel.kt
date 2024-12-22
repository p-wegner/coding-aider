package de.andrena.codingaider.toolwindow.runningcommands

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent

class RunningCommandsPanel(private val project: Project) {
    private val runningCommandsListModel = project.service<RunningCommandService>().getRunningCommandsListModel()
    private val runningCommandsList = JBList(runningCommandsListModel).apply {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    selectedValue?.let { command ->
                        command.focus()
                    }
                }
            }
        })
    }

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
