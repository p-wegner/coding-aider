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
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

class PlanViewer(private val project: Project) {
    private val plansListModel = DefaultListModel<AiderPlan>()
    private val expandedPlans = mutableSetOf<String>()
    val plansList = JBList(plansListModel)

    init {
        plansList.run {
            cellRenderer = PlanListCellRenderer(false, expandedPlans)

            // Enable keyboard navigation
            inputMap.put(KeyStroke.getKeyStroke("LEFT"), "collapse")
            inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "expand")
            inputMap.put(KeyStroke.getKeyStroke("ENTER"), "open")

            actionMap.put("collapse", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    val selectedPlan = selectedValue ?: return
                    val planId = selectedPlan.mainPlanFile?.filePath ?: return
                    if (expandedPlans.contains(planId)) {
                        expandedPlans.remove(planId)
                        updatePlans(project.getService(AiderPlanService::class.java).getAiderPlans())
                    }
                }
            })

            actionMap.put("expand", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    val selectedPlan = selectedValue ?: return
                    val planId = selectedPlan.mainPlanFile?.filePath ?: return
                    if (selectedPlan.childPlans.isNotEmpty() && !expandedPlans.contains(planId)) {
                        expandedPlans.add(planId)
                        updatePlans(project.getService(AiderPlanService::class.java).getAiderPlans())
                    }
                }
            })

            actionMap.put("open", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    val selectedPlan = selectedValue ?: return
                    selectedPlan.mainPlanFile?.let { fileData ->
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                        virtualFile?.let {
                            FileEditorManager.getInstance(project).openFile(it, true)
                        }
                    }
                }
            })

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val index = plansList.locationToIndex(e.point)
                    if (index >= 0) {
                        val cellBounds = plansList.getCellBounds(index, index)
                        if (cellBounds?.contains(e.point) == true) {
                            val plan = plansList.model.getElementAt(index)
                            val planId = plan.mainPlanFile?.filePath ?: return

                            // Calculate click areas
                            val depth = plan.depth
                            val indentWidth = 20
                            val iconWidth = 16
                            val treeAreaWidth = (depth * indentWidth) + iconWidth

                            // Check if click was in the expand/collapse icon area
                            if (e.x < treeAreaWidth + 24 && plan.childPlans.isNotEmpty()) {
                                animateTreeExpansion(plan, planId)
                            } else if (e.clickCount == 2) {
                                // Double click to open plan file
                                plan.mainPlanFile?.let { fileData ->
                                    val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                                    virtualFile?.let {
                                        FileEditorManager.getInstance(project).openFile(it, true)
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    private fun animateTreeExpansion(plan: AiderPlan, planId: String) {
        val isExpanding = !expandedPlans.contains(planId)
        val timer = Timer(20, null)
        var progress = 0
        val steps = 5 // Number of animation steps

        timer.addActionListener { e ->
            progress++
            if (progress <= steps) {
                if (isExpanding) {
                    // Gradually show children
                    expandedPlans.add(planId)
                    updatePlansWithAnimation(
                        project.getService(AiderPlanService::class.java).getAiderPlans(),
                        progress.toFloat() / steps
                    )
                } else {
                    // Gradually hide children
                    updatePlansWithAnimation(
                        project.getService(AiderPlanService::class.java).getAiderPlans(),
                        (steps - progress).toFloat() / steps
                    )
                }
            } else {
                if (!isExpanding) {
                    expandedPlans.remove(planId)
                    updatePlansWithAnimation(project.getService(AiderPlanService::class.java).getAiderPlans(), 0f)
                }
                timer.stop()
            }
        }
        timer.start()
    }

    fun updatePlans(plans: List<AiderPlan>) {
        updatePlansWithAnimation(plans, 1f)
    }

    private fun updatePlansWithAnimation(plans: List<AiderPlan>, animationProgress: Float = 1f) {
        plansListModel.clear()
        fun addPlanAndChildren(plan: AiderPlan) {
            plansListModel.addElement(plan)
            if (expandedPlans.contains(plan.mainPlanFile?.filePath)) {
                plan.childPlans.forEach { childPlan ->
                    // Apply animation progress to child indentation
                    val animatedPlan = childPlan.copy(
                        depth = (childPlan.depth * animationProgress).toInt()
                    )
                    addPlanAndChildren(animatedPlan)
                }
            }
        }
        plans.forEach { plan -> addPlanAndChildren(plan) }
    }

    class PlanListCellRenderer(
        private val shortTooltip: Boolean = true,
        private val expandedPlans: Set<String>
    ) : JPanel(BorderLayout()), ListCellRenderer<AiderPlan> {
        private val label = JLabel()
        private val statusIcon = JLabel()
        private val countLabel = JLabel()
        private val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        private val treeIndentWidth = 16 // Standard tree indent width

        init {
            isOpaque = true
            val contentPanel = JPanel(BorderLayout(4, 0))
            contentPanel.isOpaque = false
            leftPanel.isOpaque = false

            // Ensure consistent icon sizing
            statusIcon.preferredSize = Dimension(16, 16)
            leftPanel.add(statusIcon)

            // Add right padding to count label
            countLabel.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)

            contentPanel.add(leftPanel, BorderLayout.WEST)
            contentPanel.add(label, BorderLayout.CENTER)
            contentPanel.add(countLabel, BorderLayout.EAST)

            add(contentPanel, BorderLayout.CENTER)
            border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
        }

        override fun getListCellRendererComponent(
            list: JList<out AiderPlan>,
            value: AiderPlan,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val isHovered = index == (list as? JBList<*>)?.selectedIndex
            val isDark = !JBColor.isBright()
            // Enhanced visual feedback for selection and hover
            background = when {
                isSelected -> list?.selectionBackground
                isHovered -> if (isDark)
                    list?.selectionBackground?.darker()?.darker()?.brighter()
                else
                    list?.selectionBackground?.brighter()?.brighter()

                else -> list?.background
            }

            // Add subtle border for better hierarchy visualization
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                    0, 0, 1, 0, if (isDark)
                        JBColor(Color(60, 60, 60), Color(50, 50, 50))
                    else
                        JBColor(Color(230, 230, 230), Color(60, 60, 60))
                ),
                border
            )
            label.background = background
            label.foreground = when {
                isSelected -> list?.selectionForeground
                isHovered -> if (isDark) list?.foreground?.brighter() else list?.foreground?.darker()
                else -> list?.foreground
            }

            if (value != null) {
                val planFile = value.planFiles.firstOrNull()
                val fileName = planFile?.filePath?.let { File(it).nameWithoutExtension } ?: "Unknown Plan"

                // Calculate tree structure with enhanced visual representation
                val treePrefix = buildString {
                    val ancestors = value.getAncestors()
                    val maxDepth = 8 // Maximum recommended nesting depth

                    // Draw connecting lines for ancestors with improved visibility
                    ancestors.forEachIndexed { index, ancestor ->
                        val hasNextSibling = ancestor.findSiblingPlans().any { sibling ->
                            sibling.mainPlanFile?.filePath?.compareTo(ancestor.mainPlanFile?.filePath ?: "") ?: 0 > 0
                        }
                        if (index >= maxDepth - 1) {
                            append(if (hasNextSibling) "┆   " else "    ") // Dotted line for deep nesting
                        } else {
                            append(if (hasNextSibling) "┃   " else "    ") // Bold line for better visibility
                        }
                    }

                    val depth = ancestors.size
                    val isLastChild = value.parentPlan?.childPlans?.lastOrNull() == value
                    val hasChildren = value.childPlans.isNotEmpty()

                    // Add current node connector with enhanced visibility
                    if (depth >= maxDepth) {
                        append(if (isLastChild) "┗━━ " else "┣━━ ") // Double line for better visibility
                    } else {
                        append(if (isLastChild) "┗━━ " else "┣━━ ")
                    }

                    // Add clear expand/collapse indicator with spacing
                    if (hasChildren) {
                        val isExpanded = expandedPlans.contains(value.mainPlanFile?.filePath)
                        append(if (isExpanded) "▼  " else "▶  ") // Added extra space for better readability
                    } else {
                        append("   ") // Consistent spacing for leaf nodes
                    }
                }

                // Set consistent tree-like indentation
                val baseIndent = 4
                val depthIndent = value.depth * treeIndentWidth
                border = BorderFactory.createEmptyBorder(2, baseIndent + depthIndent, 2, 4)

                // Set label text with tree prefix and plan name
                label.text = treePrefix + fileName

                // Ensure consistent font
                label.font = UIManager.getFont("Tree.font")
                countLabel.font = label.font

                val openItems = value.openChecklistItems().size
                val totalItems = value.totalChecklistItems()
                val tooltip = if (shortTooltip) value.createShortTooltip() else value.createTooltip()
                toolTipText = tooltip
                label.toolTipText = tooltip
                countLabel.toolTipText = tooltip

                statusIcon.icon = when {
                    value.isPlanComplete() -> AllIcons.Actions.Commit
                    value.openChecklistItems().isEmpty() && value.childPlans.any { !it.isPlanComplete() } ->
                        AllIcons.General.Warning // Has uncompleted child plans
                    else -> AllIcons.General.BalloonInformation
                }
                statusIcon.toolTipText = tooltip

                val checkedItems = totalItems - openItems
                countLabel.text = "($checkedItems/$totalItems)"
                // Use more subtle progress colors that work well in both themes
                countLabel.foreground = when {
                    openItems == 0 -> UIManager.getColor("Label.foreground")
                    openItems < totalItems / 2 -> JBColor(
                        Color(76, 175, 80),  // Light theme - Material Design green
                        Color(129, 199, 132) // Dark theme - Lighter green
                    )

                    else -> JBColor(
                        Color(255, 152, 0),  // Light theme - Material Design orange
                        Color(255, 183, 77)  // Dark theme - Lighter orange
                    )
                }
                // Add subtle background for better visibility
                countLabel.isOpaque = true
                countLabel.background = if (isSelected) list?.selectionBackground else list?.background
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
                private val messageArea = JTextArea().apply {
                    lineWrap = true
                    wrapStyleWord = true
                    rows = 5
                    font = UIManager.getFont("TextField.font")
                    border = UIManager.getBorder("TextField.border")
                }
                private val markdownViewer = CustomMarkdownViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
                    setDarkTheme(!JBColor.isBright())
                    setMarkdownContent(selectedPlan.plan)
                }

                init {
                    title = "Refine Plan"
                    init()
                }

                override fun createCenterPanel(): JComponent {
                    val messageScrollPane = JBScrollPane(messageArea)
                    val previewScrollPane = JBScrollPane(markdownViewer.component).apply {
                        preferredSize = Dimension(600, 300)
                    }

                    return panel {
                        group("Current Plan") {
                            row {
                                cell(previewScrollPane)
                                    .align(Align.FILL)
                                    .resizableColumn()
                            }.resizableRow()
                        }

                        group("Refinement Request:") {
                            row {
                                cell(messageScrollPane)
                                    .align(Align.FILL)
                                    .resizableColumn()
                                    .comment(
                                        """
                                        Describe how you want to refine or extend the plan.
                                        This may create subplans if the changes are substantial.
                                        Use multiple lines for complex requests.
                                    """.trimIndent()
                                    )
                            }.resizableRow()
                        }
                    }
                }

                override fun getPreferredFocusedComponent(): JComponent = messageArea

                fun getMessage(): String {
                    return messageArea.text.trim()
                }
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
                    val previewScrollPane = JBScrollPane(markdownViewer.component).apply {
                        preferredSize = Dimension(600, 300)
                    }

                    return panel {
                        group("Current Plan") {
                            row {
                                cell(previewScrollPane)
                                    .align(Align.FILL)
                                    .resizableColumn()
                            }.resizableRow()
                        }

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
