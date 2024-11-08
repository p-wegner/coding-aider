package de.andrena.codingaider.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.panels.Wrapper
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderPlan
import de.andrena.codingaider.services.AiderPlanService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import com.intellij.openapi.ui.Messages
import java.awt.*
import java.io.File
import javax.swing.*

class PersistentFilesToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val persistentFilesComponent = PersistentFilesComponent(project)
        val content = ContentFactory.getInstance().createContent(persistentFilesComponent.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class PersistentFilesComponent(private val project: Project) {
    private val persistentFileService = project.getService(PersistentFileService::class.java)
    private val aiderPlanService = project.getService(AiderPlanService::class.java)
    private val persistentFilesListModel = DefaultListModel<FileData>()
    private val plansListModel = DefaultListModel<AiderPlan>()
    private val persistentFilesList: JBList<FileData> = JBList(persistentFilesListModel).apply {
        cellRenderer = PersistentFileRenderer()
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                    removeSelectedFiles()
                }
            }
        })
    }

    private val plansList = JBList(plansListModel)



    init {
        loadPersistentFiles()
        loadPlans()
        subscribeToChanges()
        subscribeToFileChanges()
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

    private fun loadPlans() {
        plansListModel.clear()
        aiderPlanService.getAiderPlans().forEach { plan ->
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
                
                // Create detailed tooltip
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
                
                // Set tooltip for all components
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
                    else -> Color(255, 140, 0) // Orange for many open items
                }
            }
            
            return this
        }
    }
    
    private fun executeSelectedPlan() {
        val selectedPlan = plansList.selectedValue ?: run {
            Messages.showWarningDialog(project, "Please select a plan to execute", "No Plan Selected")
            return
        }
        
        val settings = getInstance()
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


    private fun subscribeToChanges() {
        project.messageBus.connect().subscribe(
            PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC,
            object : PersistentFilesChangedTopic {
                override fun onPersistentFilesChanged() = loadPersistentFiles()
            }
        )
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
            group("Persistent Files") {
                row {
                    scrollCell(persistentFilesList)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }
                row {
                    button("Add Files") { addPersistentFiles() }
                    button("Add Open Files") { addOpenFilesToPersistent() }
                    button("Toggle Read-Only") { toggleReadOnlyMode() }
                    button("Remove Files") { removeSelectedFiles() }
                }
            }.resizableRow()
            
            group("Aider Plans") {
                row {
                    val toolbar = ActionManager.getInstance().createActionToolbar(
                        "AiderPlansToolbar",
                        DefaultActionGroup().apply { add(ContinuePlanAction()) },
                        true
                    )
                    toolbar.targetComponent = plansList
                    cell(Wrapper(toolbar.component))
                }
                row {
                    scrollCell(plansList)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }
            }.resizableRow()
        }
    }

    private fun addPersistentFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
        val files = FileChooser.chooseFiles(descriptor, project, null)
        val fileDataList = files.flatMap { file: com.intellij.openapi.vfs.VirtualFile ->
            if (file.isDirectory) {
                file.children.filter { it.isValid && !it.isDirectory }.map { FileData(it.path, false) }
            } else {
                listOf(FileData(file.path, false))
            }
        }
        persistentFileService.addAllFiles(fileDataList)
        loadPersistentFiles()
    }

    private fun toggleReadOnlyMode() {
        val selectedFiles = persistentFilesList.selectedValuesList
        selectedFiles.forEach { fileData ->
            val updatedFileData = fileData.copy(isReadOnly = !fileData.isReadOnly)
            persistentFileService.updateFile(updatedFileData)
        }
        loadPersistentFiles()
    }

    private fun removeSelectedFiles() {
        val selectedFiles = persistentFilesList.selectedValuesList
        persistentFileService.removePersistentFiles(selectedFiles.map { it.filePath })
        loadPersistentFiles()
    }

    private inner class ContinuePlanAction : AnAction(
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

    private fun loadPersistentFiles() {
        persistentFilesListModel.clear()
        persistentFileService.getPersistentFiles().forEach { file ->
            persistentFilesListModel.addElement(file)
        }
    }

    private fun addOpenFilesToPersistent() {
        val openFiles = FileEditorManager.getInstance(project).openFiles
        val fileDataList = openFiles.map { FileData(it.path, false) }
        persistentFileService.addAllFiles(fileDataList)
        loadPersistentFiles()
    }

    private inner class PersistentFileRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is FileData) {
                val file = File(value.filePath)
                component.text = "${file.nameWithoutExtension} ${if (value.isReadOnly) "(Read-Only)" else ""}"
                component.toolTipText = value.filePath
            }
            return component
        }
    }
}

