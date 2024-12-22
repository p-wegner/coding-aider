package de.andrena.codingaider.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.toolwindow.runningcommands.RunningCommandsPanel
import de.andrena.codingaider.toolwindow.persistentfiles.PersistentFilesPanel
import de.andrena.codingaider.toolwindow.plans.PlansPanel
import javax.swing.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class CodingAiderToolWindow : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setToHideOnEmptyContent(false)
        toolWindow.setAutoHide(false)
        val toolWindowContent = CodingAiderToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class CodingAiderToolWindowContent(project: Project) {
    private var persistentFilesCollapsed = false
    private var plansCollapsed = false 
    private var runningCommandsCollapsed = false

    private val runningCommandsPanel = RunningCommandsPanel(project)
    private val plansPanel = PlansPanel(project)
    private val persistentFilesPanel = PersistentFilesPanel(project)

    private val persistentFilesCollapsible = CollapsiblePanel(
        "Persistent Files",
        ::persistentFilesCollapsed,
        persistentFilesPanel.getContent()
    )

    private val plansCollapsible = CollapsiblePanel(
        "Plans",
        ::plansCollapsed,
        plansPanel.getContent()
    )

    private val runningCommandsCollapsible = CollapsiblePanel(
        "Running Commands", 
        ::runningCommandsCollapsed,
        runningCommandsPanel.getContent()
    )

    fun getContent(): JComponent {
        return panel {
            indent {
                row {
                    cell(persistentFilesCollapsible.headerPanel)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                }
                row {
                    cell(persistentFilesCollapsible.contentPanel)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }.topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
                
                row {
                    cell(plansCollapsible.headerPanel)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                }
                row {
                    cell(plansCollapsible.contentPanel)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }.topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
                
                row {
                    cell(runningCommandsCollapsible.headerPanel)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                }
                row {
                    cell(runningCommandsCollapsible.contentPanel)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }.topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
            }
        }
    }
}
