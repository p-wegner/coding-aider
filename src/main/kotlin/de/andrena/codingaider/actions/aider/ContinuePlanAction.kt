package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.outputview.MarkdownJcefViewer
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.ContinuePlanService
import de.andrena.codingaider.toolwindow.plans.PlanViewer
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

class ContinuePlanAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = SelectPlanDialog(project)
        if (dialog.showAndGet()) {
            dialog.selectedPlan?.run { project.service<ContinuePlanService>().continuePlan(this) }
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
        val planComboBox = ComboBox(DefaultComboBoxModel(unfinishedPlans.toTypedArray())).apply {
            renderer = PlanViewer.PlanListCellRenderer(false, expandedPlans = emptySet())
            if (unfinishedPlans.isNotEmpty()) {
                selectedIndex = 0
                selectedPlan = selectedItem as AiderPlan
            }
        }

        val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
            setDarkTheme(!JBColor.isBright())
        }

        val scrollPane = JBScrollPane(markdownViewer.component).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            border = BorderFactory.createLineBorder(JBColor.border())
        }

        planComboBox.addActionListener {
            selectedPlan = planComboBox.selectedItem as? AiderPlan
            markdownViewer.setMarkdown(selectedPlan?.plan ?: "")
        }

        // Set initial text
        markdownViewer.setMarkdown((planComboBox.selectedItem as? AiderPlan)?.plan ?: "")

        return panel {
            row("Plan") {
                cell(planComboBox)
                    .align(AlignX.FILL)
            }
            row("Details") {
                cell(scrollPane)
                    .align(AlignX.FILL)
                    .resizableColumn()
            }.resizableRow()
        }.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            preferredSize = Dimension(800, 600)
            minimumSize = Dimension(400, 300)
        }

    }
}
