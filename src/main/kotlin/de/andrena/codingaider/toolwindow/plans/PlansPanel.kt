package de.andrena.codingaider.toolwindow.plans

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.plans.AiderPlanService
import javax.swing.JComponent

class PlansPanel(private val project: Project) {
    private val aiderPlanService = project.getService(AiderPlanService::class.java)
    private val planViewer = PlanViewer(project)

    init {
        loadPlans()
        subscribeToFileChanges()
    }

    private fun loadPlans() {
        planViewer.updatePlans(aiderPlanService.getAiderPlans())
    }

    private fun subscribeToFileChanges() {
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val plansPath = "${project.basePath}/${AiderPlanService.AIDER_PLANS_FOLDER}"
                val affectsPlanFiles = events.any { event ->
                    event.path.startsWith(plansPath) && event.path.endsWith(".md")
                }
                if (affectsPlanFiles) {
                    loadPlans()
                }
            }
        })
    }

    fun getContent(): JComponent {
        return panel {
            group {
                row {
                    val toolbar = ActionManager.getInstance().createActionToolbar(
                        "AiderPlansToolbar",
                        DefaultActionGroup().apply {
                            add(planViewer.NewPlanAction())
                            addSeparator()
                            add(planViewer.RefreshPlansAction())
                            add(planViewer.ContinuePlanAction())
                            add(planViewer.RefinePlanAction())
                            add(planViewer.EditContextAction())
                            add(planViewer.DeletePlanAction())
                        },
                        true
                    )
                    toolbar.targetComponent = planViewer.plansList
                    cell(Wrapper(toolbar.component))
                }
                row {
                    scrollCell(planViewer.plansList)
                        .align(Align.FILL)
                        .resizableColumn()
                }
            }.resizableRow()
        }
    }
}
