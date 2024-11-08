package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.AiderPlan
import de.andrena.codingaider.services.AiderPlanService
import de.andrena.codingaider.toolwindow.PlanViewer
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent

class ContinuePlanAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = SelectPlanDialog(project)
        if (dialog.showAndGet()) {
            dialog.selectedPlan?.let { plan ->
                PlanViewer(project).executeSelectedPlan()
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
    private val unfinishedPlans = planService.getPlans().filter { !it.isPlanComplete() }
    private val planComboBox = JBList(unfinishedPlans)
    
    var selectedPlan: AiderPlan? = null

    init {
        title = "Select Plan to Continue"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Select a plan to continue:") {
                cell(JComboBox(DefaultComboBoxModel(unfinishedPlans.toTypedArray())).apply {
                    renderer = PlanViewer.PlanListCellRenderer()
                    addActionListener {
                        selectedPlan = selectedItem as? AiderPlan
                    }
                    if (unfinishedPlans.isNotEmpty()) {
                        selectedIndex = 0
                        selectedPlan = selectedItem as AiderPlan
                    }
                })
            }
        }
    }
}
