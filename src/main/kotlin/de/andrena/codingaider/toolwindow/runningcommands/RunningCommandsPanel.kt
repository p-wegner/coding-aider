package de.andrena.codingaider.toolwindow.runningcommands

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.actions.ide.ShowLastCommandResultAction
import de.andrena.codingaider.services.RunningCommandService
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent

class RunningCommandsPanel(private val project: Project) {
    private val runningCommandsListModel = project.service<RunningCommandService>().getRunningCommandsListModel()
    private val runningCommandsList = JBList(runningCommandsListModel).apply {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    (selectedValue)?.focus()
                }
            }
        })
    }

    private val showLastCommandResultButton = JButton("Show Last Command Result").apply {
        addActionListener {
            ShowLastCommandResultAction().showLastCommandFor(project)
        }
    }

    fun getContent(): JComponent {
        return panel {
            group("Running Commands") {
                row {
                    scrollCell(runningCommandsList)
                        .align(Align.FILL)
                        .resizableColumn()
                }
                row {
                    cell(showLastCommandResultButton)
                }
            }
        }
    }
}
