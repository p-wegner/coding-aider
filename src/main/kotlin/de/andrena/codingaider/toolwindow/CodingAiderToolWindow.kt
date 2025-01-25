package de.andrena.codingaider.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.toolwindow.persistentfiles.PersistentFilesPanel
import de.andrena.codingaider.toolwindow.plans.PlansPanel
import de.andrena.codingaider.toolwindow.runningcommands.RunningCommandsPanel
import javax.swing.JComponent

class CodingAiderToolWindow : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setToHideOnEmptyContent(false)
        toolWindow.isAutoHide = false
        val toolWindowContent = CodingAiderToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class CodingAiderToolWindowContent(project: Project) {
    private val runningCommandsPanel = RunningCommandsPanel(project)
    private val plansPanel = PlansPanel(project)
    private val persistentFilesPanel = PersistentFilesPanel(project)
    private val workingDirectoryPanel = WorkingDirectoryPanel(project)

    fun getContent(): JComponent {
        return panel {
            indent {
                collapsibleGroup("Persistent Files") {
                    row {
                        cell(persistentFilesPanel.getContent())
                            .align(com.intellij.ui.dsl.builder.Align.FILL)
                            .resizableColumn()
                    }
                }.apply { expanded = true }
                    .topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)

                collapsibleGroup("Plans") {
                    row {
                        cell(plansPanel.getContent())
                            .align(com.intellij.ui.dsl.builder.Align.FILL)
                            .resizableColumn()
                    }
                }
                    .apply { expanded = true }
                    .topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)

                collapsibleGroup("Working Directory") {
                    row {
                        cell(workingDirectoryPanel.getContent())
                            .align(com.intellij.ui.dsl.builder.Align.FILL)
                            .resizableColumn()
                    }
                }
                    .apply { expanded = true }
                    .topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)

                collapsibleGroup("Running Commands") {
                    row {
                        cell(runningCommandsPanel.getContent())
                            .align(com.intellij.ui.dsl.builder.Align.FILL)
                            .resizableColumn()
                    }
                }
                    .apply { expanded = true }
                    .topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
            }
        }
    }
}
