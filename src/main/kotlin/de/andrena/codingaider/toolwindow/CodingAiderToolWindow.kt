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
    private val runningCommandsPanel = RunningCommandsPanel(project)
    private val plansPanel = PlansPanel(project)
    private val persistentFilesPanel = PersistentFilesPanel(project)

    fun getContent(): JComponent {
        return panel {
            indent {
                row {
                    cell(persistentFilesPanel.getContent())
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }.topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
                
                row {
                    cell(plansPanel.getContent())
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }.topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
                
                row {
                    cell(runningCommandsPanel.getContent())
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }.topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
            }
        }
    }
}
