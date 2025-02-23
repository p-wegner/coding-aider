package de.andrena.codingaider.toolwindow.plans

import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Actions.SuggestedRefactoringBulb
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
import de.andrena.codingaider.outputview.MarkdownJcefViewer
import de.andrena.codingaider.services.FileDataCollectionService
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
                        
                        // Adjust list height after expansion
                        val visibleRowCount = minOf(plansListModel.size(), 10) // Show max 10 rows
                        plansList.visibleRowCount = visibleRowCount
                        plansList.parent?.let { parent ->
                            parent.preferredSize = plansList.preferredSize
                            if (parent is JViewport) {
                                parent.viewSize = plansList.preferredSize
                            }
                        }
                        plansList.revalidate()
                        plansList.repaint()
                    }
                }
            })

            actionMap.put("open", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    val selectedPlan = selectedValue ?: return
                    selectedPlan.mainPlanFile?.let { fileData ->
                        val file = File(fileData.filePath)
                        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                        virtualFile?.let {
                            FileEditorManager.getInstance(project).openFile(it, true)
                        }
                    }
                }
            })

            addMouseListener(object : MouseAdapter() {
                private var hoveredPlan: AiderPlan? = null
                
                override fun mouseMoved(e: MouseEvent) {
                    val index = plansList.locationToIndex(e.point)
                    if (index >= 0) {
                        val plan = plansList.model.getElementAt(index)
                        val planId = plan.mainPlanFile?.filePath ?: return
                        
                        // Calculate if mouse is over expand/collapse area
                        val depth = plan.depth
                        val indentWidth = 20
                        val iconWidth = 16
                        val treeAreaWidth = (depth * indentWidth) + iconWidth
                        val expandClickWidth = treeAreaWidth + 24
                        
                        if (e.x < expandClickWidth && plan.childPlans.isNotEmpty()) {
                            if (hoveredPlan != plan) {
                                hoveredPlan = plan
                                plansList.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                plansList.repaint()
                            }
                        } else {
                            if (hoveredPlan != null) {
                                hoveredPlan = null
                                plansList.cursor = Cursor.getDefaultCursor()
                                plansList.repaint()
                            }
                        }
                    }
                }
                
                override fun mouseExited(e: MouseEvent) {
                    if (hoveredPlan != null) {
                        hoveredPlan = null
                        plansList.cursor = Cursor.getDefaultCursor()
                        plansList.repaint()
                    }
                }

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
                            val expandClickWidth = treeAreaWidth + 24

                            // Check if click was in the expand/collapse icon area
                            if (e.x < expandClickWidth && plan.childPlans.isNotEmpty()) {
                                animateTreeExpansion(plan, planId)
                            } else if (e.clickCount >= 2) {
                                // Double click to open plan file
                                plansList.selectedIndex = index
                                plan.mainPlanFile?.let { fileData ->
                                    val file = File(fileData.filePath)
                                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
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
        val timer = Timer(12, null) // Faster animation (approximately 83fps)
        var progress = 0
        val steps = 12 // More steps for smoother animation
        
        // Store initial height for animation
        val initialHeight = plansList.preferredSize.height
        val targetHeight = if (isExpanding) {
            // Calculate target height based on number of visible items
            val visibleRowCount = minOf(plansListModel.size() + plan.childPlans.size, 10)
            plansList.visibleRowCount = visibleRowCount
            plansList.preferredSize.height
        } else {
            initialHeight - (plan.childPlans.size * plansList.fixedCellHeight)
        }

        fun easeInOutQuad(t: Float): Float {
            return if (t < 0.5f) 2 * t * t else -1 + (4 - 2 * t) * t
        }

        timer.addActionListener { e ->
            progress++
            if (progress <= steps) {
                val normalizedProgress = progress.toFloat() / steps
                val easedProgress = easeInOutQuad(normalizedProgress)
                
                if (isExpanding) {
                    expandedPlans.add(planId)
                    updatePlansWithAnimation(
                        project.getService(AiderPlanService::class.java).getAiderPlans(),
                        easedProgress
                    )
                } else {
                    updatePlansWithAnimation(
                        project.getService(AiderPlanService::class.java).getAiderPlans(),
                        1 - easedProgress
                    )
                }
            } else {
                if (!isExpanding) {
                    expandedPlans.remove(planId)
                    updatePlansWithAnimation(project.getService(AiderPlanService::class.java).getAiderPlans(), 0f)
                }
                
                // Final height adjustment
                plansList.visibleRowCount = minOf(plansListModel.size(), 10)
                plansList.parent?.let { parent ->
                    parent.preferredSize = plansList.preferredSize
                    if (parent is JViewport) {
                        parent.viewSize = plansList.preferredSize
                    }
                }
                plansList.revalidate()
                plansList.repaint()
                
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
        
        // Create a map of all plans by their path
        val plansMap = mutableMapOf<String, AiderPlan>()
        plans.forEach { plan ->
            plansMap[plan.mainPlanFile?.filePath ?: ""] = plan
        }
        
        // Find true root plans (plans that aren't referenced as subplans by any other plan)
        val rootPlans = plans.filter { plan ->
            !plans.any { otherPlan ->
                otherPlan.childPlans.any { childPlan -> 
                    childPlan.mainPlanFile?.filePath == plan.mainPlanFile?.filePath
                }
            }
        }
        
        // Track visited plans to prevent duplicates
        val visitedPlans = mutableSetOf<String>()
        
        // Recursive function to add plans with proper hierarchy
        fun addPlanAndChildren(plan: AiderPlan, depth: Int = 0) {
            val planId = plan.mainPlanFile?.filePath ?: return
            if (planId in visitedPlans) return
            visitedPlans.add(planId)
            
            // Create a copy with correct depth
            val planWithDepth = plan.copy(depth = depth)
            plansListModel.addElement(planWithDepth)
            
            // If expanded, add all children and their descendants
            if (expandedPlans.contains(planId)) {
                plan.childPlans.forEach { child ->
                    // Only add children that haven't been visited
                    if (child.mainPlanFile?.filePath !in visitedPlans) {
                        addPlanAndChildren(child, depth + 1)
                    }
                }
            }
        }
        
        // Add all root plans and their hierarchies
        rootPlans.forEach { plan -> 
            addPlanAndChildren(plan)
        }
        
        // Ensure proper tree structure visualization
        plansList.cellRenderer = PlanListCellRenderer(false, expandedPlans)
        
        // Ensure proper tree structure visualization
        plansList.cellRenderer = PlanListCellRenderer(false, expandedPlans)
    }

    class PlanListCellRenderer(
        private val shortTooltip: Boolean = true,
        private val expandedPlans: Set<String>
    ) : JPanel(BorderLayout()), ListCellRenderer<AiderPlan> {
        private val label = JLabel()
        private val statusIcon = JLabel()
        private val countLabel = JLabel()
        private val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
        private val treeIndentWidth = 20

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
            val isHovered = index >= 0 && index == (list as? JBList<*>)?.selectedIndex
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

                // Calculate tree structure with proper hierarchy visualization
                val treePrefix = buildString {
                    val ancestors = value.getAncestors()
                    
                    // Draw tree structure
                    val hasChildren = value.childPlans.isNotEmpty()
                    val isExpanded = expandedPlans.contains(value.mainPlanFile?.filePath)
                    
                    // Draw connecting lines for ancestors
                    ancestors.forEach { ancestor ->
                        // Check if this ancestor has siblings after it
                        val hasNextSibling = ancestor.findSiblingPlans().any { sibling ->
                            sibling.mainPlanFile?.filePath?.compareTo(ancestor.mainPlanFile?.filePath ?: "") ?: 0 > 0
                        }
                        append(if (hasNextSibling) "│   " else "    ")
                    }

                    // Check if this is the last child in its parent's children
                    val isLastChild = value.parentPlan?.childPlans?.lastOrNull() == value
                    
                    // Add current node connector with hover effect
                    val isHovered = value == (list as? JBList<*>)?.let { 
                        val selectedIdx = it.selectedIndex
                        if (selectedIdx >= 0 && selectedIdx < it.model.size) {
                            it.model.getElementAt(selectedIdx)
                        } else {
                            null
                        }
                    }
                
                    // Add expand/collapse indicator if has children
                    if (hasChildren) {
                        val iconColor = if (isHovered) {
                            if (isDark) "#64B5F6" else "#1976D2" // Blue color for hover
                        } else {
                            if (isDark) "#BDBDBD" else "#616161" // Gray color
                        }
                    
                        append(if (isExpanded) """<font color="$iconColor">▼</font>""" 
                               else """<font color="$iconColor">▶</font>""")
                        append(" ")
                    } else {
                        append(if (isLastChild) "└──" else "├──")
                        append(" ")
                    }
                }

                // Calculate indentation based on depth
                val baseIndent = 4
                val depthIndent = value.depth * treeIndentWidth
                
                // Use standard background color for all levels
                background = if (isSelected) list?.selectionBackground else list?.background
                border = BorderFactory.createEmptyBorder(2, baseIndent + depthIndent, 2, 4)

                // Set label text with HTML formatting for colors
                label.text = "<html>$treePrefix$fileName</html>"

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
                    value.isPlanComplete() -> AllIcons.Diff.GutterCheckBoxSelected
                    value.openChecklistItems().isEmpty() && value.childPlans.any { !it.isPlanComplete() } ->
                        AllIcons.Diff.GutterCheckBoxIndeterminate // Has uncompleted child plans
                    else -> AllIcons.Diff.GutterCheckBox
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
            val allFiles = project.service<FileDataCollectionService>().collectAllFiles(includePersistentFiles = true)
            val dialog = AiderInputDialog(project, allFiles).apply {
                selectMode(AiderMode.STRUCTURED)
            }
            if (dialog.showAndGet()) {
                val commandData = AiderAction.collectCommandData(dialog, project)
                AiderAction.executeAiderActionWithCommandData(project, commandData)
            }
        }
    }


    inner class ArchivePlanAction : AnAction(
        "Archive Plan",
        "Move this plan to the finished plans folder",
        AllIcons.Actions.ClearCash
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            val selectedPlan = plansList.selectedValue ?: return

            val dialog = object : DialogWrapper(project) {
                init {
                    title = "Archive Plan"
                    init()
                }

                override fun createCenterPanel(): JComponent {
                    val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
                        setDarkTheme(!JBColor.isBright())
                        setMarkdown(selectedPlan.plan)
                    }
                    val previewScrollPane = JBScrollPane(markdownViewer.component).apply {
                        preferredSize = Dimension(600, 300)
                    }

                    return panel {
                        group("Plan to Archive") {
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
                val finishedPlansDir = File(project.basePath + "/${AiderPlanService.FINISHED_AIDER_PLANS_FOLDER}")
                if (!finishedPlansDir.exists()) {
                    finishedPlansDir.mkdir()
                }

                selectedPlan.planFiles.forEach { fileData ->
                    val file = File(fileData.filePath)
                    if (file.exists()) {
                        val destFile = File(finishedPlansDir, file.name)
                        if (destFile.exists()) {
                            destFile.delete()
                        }
                        file.renameTo(destFile)
                    }
                }
                FileRefresher.refreshPath(project.basePath + "/${AiderPlanService.AIDER_PLANS_FOLDER}")
                FileRefresher.refreshPath(finishedPlansDir.absolutePath)
                updatePlans(project.getService(AiderPlanService::class.java).getAiderPlans())
            }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = plansList.selectedValue != null
        }
    }

    inner class RefinePlanAction : AnAction(
        "Refine Plan",
        "Refine and extend the selected plan",
        SuggestedRefactoringBulb
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            val selectedPlan = plansList.selectedValue ?: return

            val dialog = AiderPlanRefinementDialog(project, selectedPlan)

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
                    val markdownViewer = MarkdownJcefViewer(listOf(AiderPlanService.AIDER_PLANS_FOLDER)).apply {
                        setDarkTheme(!JBColor.isBright())
                        setMarkdown(selectedPlan.plan)
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
