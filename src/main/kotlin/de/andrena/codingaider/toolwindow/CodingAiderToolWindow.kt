package de.andrena.codingaider.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.toolwindow.persistentfiles.PersistentFilesPanel
import de.andrena.codingaider.toolwindow.plans.PlansPanel
import de.andrena.codingaider.toolwindow.runningcommands.RunningCommandsPanel
import de.andrena.codingaider.toolwindow.workingdirectory.WorkingDirectoryPanel
import com.intellij.ui.components.JBScrollPane
import de.andrena.codingaider.startup.McpServerStartupActivity
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

class CodingAiderToolWindowContent(private val project: Project) {
    private val settings = AiderSettings.getInstance()
    private val runningCommandsPanel = RunningCommandsPanel(project)
    private val plansPanel = PlansPanel(project)
    private val persistentFilesPanel = PersistentFilesPanel(project)
    private val workingDirectoryPanel = WorkingDirectoryPanel(project)
    private var contentPanel: JComponent? = null

    init {
        // Listen for settings changes to update the tool window reactively
        settings.addSettingsChangeListener {
            refreshContent()
        }
        // hack to ensure the MCP server is initialized when the tool window is created
        project.getService(McpServerStartupActivity::class.java)
    }

    fun getContent(): JComponent {
        if (contentPanel == null) {
            contentPanel = createContentPanel()
        }
        return contentPanel!!
    }

    private fun createContentPanel(): JComponent {
        val contentPanel = panel {
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

                if (settings.showWorkingDirectoryPanel) {
                    collapsibleGroup("Working Directory") {
                        row {
                            cell(workingDirectoryPanel.getContent())
                                .align(com.intellij.ui.dsl.builder.Align.FILL)
                                .resizableColumn()
                        }
                    }
                        .apply { expanded = true }
                        .topGap(com.intellij.ui.dsl.builder.TopGap.SMALL)
                }

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
        return JBScrollPane(contentPanel)
    }

    private fun refreshContent() {
        contentPanel?.let { oldPanel ->
            val parent = oldPanel.parent
            if (parent != null) {
                val newPanel = createContentPanel()
                parent.remove(oldPanel)
                parent.add(newPanel)
                contentPanel = newPanel
                parent.revalidate()
                parent.repaint()
            }
        }
    }
}
