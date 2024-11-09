package de.andrena.codingaider.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.toolwindow.components.PersistentFilesPanel
import de.andrena.codingaider.toolwindow.components.PlansPanel
import javax.swing.JComponent

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
