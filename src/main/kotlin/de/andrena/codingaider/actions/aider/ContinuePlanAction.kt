package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.AiderPlan
import de.andrena.codingaider.services.AiderPlanService
import de.andrena.codingaider.toolwindow.PlanViewer
import com.intellij.ui.JBColor
import de.andrena.codingaider.outputview.CustomMarkdownViewer
import com.intellij.ui.components.JBScrollPane
import java.awt.Dimension
import javax.swing.*

class ContinuePlanAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = SelectPlanDialog(project)
        if (dialog.showAndGet()) {
            val selectedPlan = dialog.selectedPlan
            if (selectedPlan != null) {
                val planViewer = PlanViewer(project)
                planViewer.plansList.setSelectedValue(selectedPlan, true)
                planViewer.executeSelectedPlan()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

private class SelectPlanDialog(private val project: Project) : DialogWrapper(project) {
    private val planService = project.getService(AiderPlanService::class.java)
    private val unfinishedPlans = planService.getAiderPlans().filter { !it.isPlanComplete() }
    
    var selectedPlan: AiderPlan? = null

    init {
        title = "Select Plan to Continue"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val planComboBox = JComboBox(DefaultComboBoxModel(unfinishedPlans.toTypedArray())).apply {
            renderer = PlanViewer.PlanListCellRenderer()
            if (unfinishedPlans.isNotEmpty()) {
                selectedIndex = 0
                selectedPlan = selectedItem as AiderPlan
            }
        }

        val markdownViewer = CustomMarkdownViewer().apply {
            setDarkTheme(!JBColor.isBright())
        }

        planComboBox.addActionListener {
            selectedPlan = planComboBox.selectedItem as? AiderPlan
            markdownViewer.setMarkdownContent(selectedPlan?.plan ?: "")
        }

        // Set initial text
        markdownViewer.setMarkdownContent((planComboBox.selectedItem as? AiderPlan)?.plan ?: "")

        return panel {
            row("Select a plan to continue:") {
                cell(planComboBox)
            }
            row("Plan details:") {
                cell(JBScrollPane(markdownViewer.component))
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
        }
    }
}
