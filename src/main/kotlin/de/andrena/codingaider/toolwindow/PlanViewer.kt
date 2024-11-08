package de.andrena.codingaider.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBList
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderPlan
import de.andrena.codingaider.settings.AiderSettings
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class PlanViewer(private val project: Project) {
    private val plansListModel = DefaultListModel<AiderPlan>()
    val plansList = JBList(plansListModel)

    init {
        plansList.run {
            cellRenderer = PlanListCellRenderer()
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val index = plansList.locationToIndex(e.point)
                    if (index >= 0 && e.clickCount == 2) {
                        val plan = plansList.model.getElementAt(index)
                        plan.files.forEach { fileData ->
                            val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                            if (virtualFile != null) {
                                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                            }
                        }
                    }
                }
            })
        }
    }

    fun updatePlans(plans: List<AiderPlan>) {
        plansListModel.clear()
        plans.forEach { plan ->
            plansListModel.addElement(plan)
        }
    }

    private class PlanListCellRenderer : JPanel(BorderLayout()), ListCellRenderer<AiderPlan?> {
        private val label = JLabel()
        private val statusIcon = JLabel()
        private val countLabel = JLabel()
        private val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))

        init {
            isOpaque = true
            val contentPanel = JPanel(BorderLayout(8, 0))
            contentPanel.isOpaque = false
            leftPanel.isOpaque = false
            
            leftPanel.add(statusIcon)
            
            contentPanel.add(leftPanel, BorderLayout.WEST)
            contentPanel.add(label, BorderLayout.CENTER)
            contentPanel.add(countLabel, BorderLayout.EAST)
            
            add(contentPanel, BorderLayout.CENTER)
            border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        }

        override fun getListCellRendererComponent(
            list: JList<out AiderPlan>?,
            value: AiderPlan?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            background = if (isSelected) list?.selectionBackground else list?.background
            label.background = background
            label.foreground = if (isSelected) list?.selectionForeground else list?.foreground
            
            if (value != null) {
                val planFile = value.files.firstOrNull()
                val fileName = planFile?.filePath?.let { File(it).nameWithoutExtension } ?: "Unknown Plan"
                label.text = fileName
                
                val openItems = value.openChecklistItems().size
                val totalItems = value.totalChecklistItems()
                val completionStatus = if (value.isPlanComplete()) "Complete" else "In Progress"
                val planPreview = value.plan.lines().take(3).joinToString("\n").let {
                    if (it.length > 200) it.take(200) + "..." else it
                }
                
                val tooltip = buildString {
                    appendLine("<html><body style='width: 400px'>")
                    appendLine("<b>Plan:</b> ${planFile?.filePath}<br>")
                    appendLine("<b>Status:</b> $completionStatus<br>")
                    val checkedItems = totalItems - openItems
                    appendLine("<b>Progress:</b> $checkedItems/$totalItems items completed<br>")
                    appendLine("<br><b>Open Items:</b><br>")
                    value.openChecklistItems().take(5).forEach { item ->
                        appendLine("• ${item.description.replace("<", "&lt;").replace(">", "&gt;")}<br>")
                    }
                    if (value.openChecklistItems().size > 5) {
                        appendLine("<i>... and ${value.openChecklistItems().size - 5} more items</i><br>")
                    }
                    appendLine("<br><b>Description:</b><br>")
                    appendLine(planPreview.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>"))
                    
                    if (value.isPlanComplete()) {
                        appendLine("<br><br><span style='color: green'>✓ All tasks completed!</span>")
                    }
                    appendLine("</body></html>")
                }
                
                toolTipText = tooltip
                label.toolTipText = tooltip
                countLabel.toolTipText = tooltip
                
                statusIcon.icon = if (value.isPlanComplete()) 
                    AllIcons.Actions.Commit 
                else 
                    AllIcons.General.BalloonInformation
                statusIcon.toolTipText = tooltip
                
                val checkedItems = totalItems - openItems
                countLabel.text = "($checkedItems/$totalItems)"
                countLabel.foreground = when {
                    openItems == 0 -> UIManager.getColor("Label.foreground")
                    openItems < totalItems/2 -> UIManager.getColor("Label.infoForeground")
                    else -> Color(255, 140, 0)
                }
            }
            
            return this
        }
    }

    fun executeSelectedPlan() {
        val selectedPlan = plansList.selectedValue ?: run {
            Messages.showWarningDialog(project, "Please select a plan to execute", "No Plan Selected")
            return
        }
        
        val settings = AiderSettings.getInstance()
        val commandData = CommandData(
            message = "",
            useYesFlag = settings.useYesFlag,
            llm = settings.llm,
            additionalArgs = "",
            files = selectedPlan.files,
            lintCmd = "",
            projectPath = project.basePath ?: "",
            aiderMode = AiderMode.STRUCTURED,
        )
        
        IDEBasedExecutor(project, commandData).execute()
    }

    inner class ContinuePlanAction : AnAction(
        "Continue Plan",
        "Continue executing this plan",
        AllIcons.Actions.Execute
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
        override fun actionPerformed(e: AnActionEvent) {
            executeSelectedPlan()
        }

        override fun update(e: AnActionEvent) {
            val selectedPlan = plansList.selectedValue
            e.presentation.isEnabled = selectedPlan != null && !selectedPlan.isPlanComplete()
        }
    }
}
