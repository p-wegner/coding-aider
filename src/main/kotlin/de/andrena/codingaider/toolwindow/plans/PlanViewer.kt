package de.andrena.codingaider.toolwindow.plans

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.actions.aider.AiderAction
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.outputview.CustomMarkdownViewer
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.AiderPlanPromptService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.plans.ContinuePlanService
import de.andrena.codingaider.utils.FileRefresher
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
            cellRenderer = PlanListCellRenderer(false)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val index = plansList.locationToIndex(e.point)
                    if (index >= 0 && e.clickCount == 2 && plansList.getCellBounds(index, index)
                            ?.contains(e.point) == true
                    ) {
                        val plan = plansList.model.getElementAt(index)
                        plan.mainPlanFile?.let { fileData ->
                            LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                        }?.let {
                            FileEditorManager.getInstance(project).openFile(it, true)
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

    class PlanListCellRenderer(private val shortTooltip: Boolean = true) : JPanel(BorderLayout()),
        ListCellRenderer<AiderPlan> {
        private val label = JLabel()
        private val statusIcon = JLabel()
        private val countLabel = JLabel()
        private val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        private val treeIndentWidth = 20 // Width for each level of indentation

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
            list: JList<out AiderPlan>,
            value: AiderPlan,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            background = if (isSelected) list?.selectionBackground else list?.background
            label.background = background
            label.foreground = if (isSelected) list?.selectionForeground else list?.foreground

            if (value != null) {
                val planFile = value.planFiles.firstOrNull()
                val fileName = planFile?.filePath?.let { File(it).nameWithoutExtension } ?: "Unknown Plan"

                // Calculate tree structure
                val treePrefix = buildString {
                    // Get the chain of ancestors
                    val ancestors = value.getAncestors()
                    val depth = ancestors.size

                    // For each ancestor level, add the appropriate connector
                    ancestors.forEachIndexed { index, ancestor ->
                        val hasNextSibling = ancestor.findSiblingPlans().any { sibling -> 
                            sibling.mainPlanFile?.filePath?.compareTo(ancestor.mainPlanFile?.filePath ?: "") ?: 0 > 0 
                        }
                        if (hasNextSibling) {
                            append("│   ")
                        } else {
                            append("    ")
                        }
                    }

                    // Add the current node's connector
                    if (value.childPlans.isNotEmpty()) {
                        val isLastChild = value.parentPlan?.childPlans?.lastOrNull() == value
                        append(if (isLastChild) "└─▼ " else "├─▼ ")
                    } else {
                        val isLastChild = value.parentPlan?.childPlans?.lastOrNull() == value
                        append(if (isLastChild) "└── " else "├── ")
                    }
                }

                // Set indentation using border
                border = BorderFactory.createEmptyBorder(4, 8 + (value.depth * treeIndentWidth), 4, 8)
                label.text = treePrefix + fileName

                val openItems = value.openChecklistItems().size
                val totalItems = value.totalChecklistItems()
                val tooltip = if (shortTooltip) value.createShortTooltip() else value.createTooltip()
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
                    openItems < totalItems / 2 -> UIManager.getColor("Label.infoForeground")
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

        project.service<ContinuePlanService>().continuePlan(selectedPlan)
    }

    inner class RefreshPlansAction : AnAction(
        "Refresh Plans",
        "Refresh and reparse the current plans",
        AllIcons.Actions.Refresh
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            ApplicationManager.getApplication().invokeLater {
                updatePlans(project.getService(AiderPlanService::class.java).getAiderPlans())
            }
        }
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

    inner class EditContextAction : AnAction(
        "Edit Context",
        "Edit context files for this plan",
        AllIcons.Actions.Edit
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            val selectedPlan = plansList.selectedValue ?: return
            if (selectedPlan.contextYamlFile == null) {
                val contextFilePath = selectedPlan.mainPlanFile?.filePath?.replace(".md", "_context.yaml")
                if (contextFilePath != null) {
                    File(contextFilePath).createNewFile()
                }
            }
            EditContextDialog(project, selectedPlan).show()
        }

        override fun update(e: AnActionEvent) {
            val selectedPlan = plansList.selectedValue
            e.presentation.isEnabled = selectedPlan != null && selectedPlan.mainPlanFile != null
        }
    }

    inner class NewPlanAction : AnAction(
        "New Plan",
        "Create a new plan",
        AllIcons.General.Add
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            val dialog = AiderInputDialog(project, emptyList()).apply {
                selectMode(AiderMode.STRUCTURED)
            }
            if (dialog.showAndGet()) {
                val commandData = AiderAction.collectCommandData(dialog, project)
                AiderAction.executeAiderActionWithCommandData(project, commandData)
            }
        }
    }

    inner class RefinePlanAction : AnAction(
        "Refine Plan",
        "Refine and extend the selected plan",
        AllIcons.Actions.Edit
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            val selectedPlan = plansList.selectedValue ?: return

            val dialog = object : DialogWrapper(project) {
                private val messageField = JTextField()

                init {
                    title = "Refine Plan"
                    init()
                }

                override fun createCenterPanel(): JComponent {
                    return panel {
                        row {
                            label("How would you like to refine this plan?")
                        }
                        row {
                            cell(messageField)
                                .align(Align.FILL)
                                .resizableColumn()
                                .focused()
                        }
                        row {
                            comment("Enter your request to refine or extend the plan. This may create subplans if needed.")
                        }
                    }
                }

                override fun getPreferredFocusedComponent(): JComponent = messageField

                fun getMessage(): String = messageField.text
            }

            if (dialog.showAndGet()) {
                val message = dialog.getMessage()
                if (message.isNotBlank()) {
                    val promptService = project.service<AiderPlanPromptService>()
                    val refinementPrompt = promptService.createPlanRefinementPrompt(selectedPlan, message)
                    val commandData = AiderAction.collectCommandData(
                        selectedPlan.allFiles,
                        refinementPrompt,
                        project,
                        AiderMode.NORMAL
                    )
                    AiderAction.executeAiderActionWithCommandData(project, commandData)
                }
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = plansList.selectedValue != null
        }
    }

    inner class DeletePlanAction : AnAction(
        "Delete Plan",
        "Delete this plan",
        AllIcons.Actions.GC
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            val selectedPlan = plansList.selectedValue ?: return

            val dialog = object : DialogWrapper(project) {
                init {
                    title = "Delete Plan"
                    init()
                }

                override fun createCenterPanel(): JComponent {
                    val markdownViewer = CustomMarkdownViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
                        setDarkTheme(!JBColor.isBright())
                        setMarkdownContent(selectedPlan.plan)
                    }

                    val scrollPane = JBScrollPane(markdownViewer.component).apply {
                        preferredSize = Dimension(800, 400)
                    }

                    return panel {
                        row {
                            label("Are you sure you want to delete this plan?")
                        }
                        row {
                            cell(scrollPane)
                                .align(Align.FILL)
                                .resizableColumn()
                        }.resizableRow()
                    }
                }
            }

            if (dialog.showAndGet()) {
                selectedPlan.planFiles.forEach { fileData ->
                    val file = File(fileData.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                FileRefresher.refreshPath(project.basePath + "/${AiderPlanService.AIDER_PLANS_FOLDER}")
                updatePlans(project.getService(AiderPlanService::class.java).getAiderPlans())
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = plansList.selectedValue != null
        }
    }
}
