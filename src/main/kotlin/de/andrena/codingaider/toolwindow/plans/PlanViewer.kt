package de.andrena.codingaider.toolwindow.plans

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.outputview.CustomMarkdownViewer
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings
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
                    if (index >= 0 && e.clickCount == 2 && plansList.getCellBounds(index, index)?.contains(e.point) == true) {
                        val plan = plansList.model.getElementAt(index)
                        plan.mainPlanFile?.let { fileData ->
                             LocalFileSystem.getInstance().findFileByPath(fileData.filePath)}?.let {
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
        ListCellRenderer<AiderPlan?> {
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
                val planFile = value.planFiles.firstOrNull()
                val fileName = planFile?.filePath?.let { File(it).nameWithoutExtension } ?: "Unknown Plan"
                label.text = fileName

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

        val settings = AiderSettings.getInstance()
        val virtualFiles: List<VirtualFile> =
            selectedPlan.allFiles.mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it.filePath) }
        val filesToInclude = project.service<FileDataCollectionService>().collectAllFiles(virtualFiles.toTypedArray())

        val commandData = CommandData(
            message = "",
            useYesFlag = settings.useYesFlag,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs,
            files = filesToInclude,
            lintCmd = settings.lintCmd,
            projectPath = project.basePath ?: "",
            aiderMode = AiderMode.STRUCTURED,
             sidecarMode = settings.useSidecarMode
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
