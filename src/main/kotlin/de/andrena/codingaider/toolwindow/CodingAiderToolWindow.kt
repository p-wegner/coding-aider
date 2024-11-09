package de.andrena.codingaider.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.toolwindow.persistentfiles.PersistentFilesPanel
import de.andrena.codingaider.toolwindow.plans.PlansPanel
import javax.swing.JComponent

class CodingAiderToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = CodingAiderToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class CodingAiderToolWindowContent(project: Project) {
    private val persistentFilesPanel = PersistentFilesPanel(project)
    private val plansPanel = PlansPanel(project)

    fun getContent(): JComponent {
        return panel {
            row {
                cell(persistentFilesPanel.getContent())
                    .align(com.intellij.ui.dsl.builder.Align.FILL)
                    .resizableColumn()
            }
            row {
                cell(plansPanel.getContent())
                    .align(com.intellij.ui.dsl.builder.Align.FILL)
                    .resizableColumn()
            }
        }
    }
}
