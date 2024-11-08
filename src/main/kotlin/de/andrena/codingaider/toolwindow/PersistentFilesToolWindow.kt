package de.andrena.codingaider.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
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
    private val plansListModel: DefaultListModel<AiderPlan> = DefaultListModel()
    private val persistentFilesList = JBList(persistentFilesListModel).apply {
        cellRenderer = PersistentFileRenderer()
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                    removeSelectedFiles()
                }
            }
        })
    }

    private val plansList = JBList(plansListModel).apply {
        val renderer = PlanListCellRenderer()
        cellRenderer = renderer
        
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index = locationToIndex(e.point)
                if (index != -1) {
                    val plan = plansListModel.getElementAt(index)
                    val cell = getCellBounds(index, index)
                    val renderer2 = (cellRenderer as? ListCellRenderer<*>)?.getListCellRendererComponent(
                        plansList,
                        plan,
                        index,
                        true,
                        false
                    ) as? PlanListCellRenderer
                    val buttonBounds = renderer2?.getExecuteButtonBounds() ?: Rectangle()
                    val executeButtonBounds = Rectangle(
                        cell.x + buttonBounds.x,
                        cell.y + buttonBounds.y,
                        buttonBounds.width,
                        buttonBounds.height
                    )
                    
                    if (executeButtonBounds.contains(e.point)) {
                        executeSelectedPlan()
                        return
                    }
                    
                    if (e.clickCount == 2) {
                        val planFile = plan.files.firstOrNull()
                        planFile?.let {
                            val virtualFile = LocalFileSystem.getInstance().findFileByPath(it.filePath)
                            virtualFile?.let { vf ->
                                FileEditorManager.getInstance(project).openFile(vf, true)
                            }
                        }
                    }
                }
            }
        })
        
        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val index = locationToIndex(e.point)
                if (index != -1) {
                    val cell = getCellBounds(index, index)
                    val executeButtonBounds = Rectangle(
                        cell.x + cell.width - 25,
                        cell.y,
                        20,
                        cell.height
                    )
                    renderer.showExecuteButton(executeButtonBounds.contains(e.point))
                    repaint(cell)
                }
            }
            
            override fun mouseExited(e: MouseEvent) {
                renderer.showExecuteButton(false)
                repaint()
            }
        })
    }

    private var doubleClickListener: (() -> Unit)? = null

    init {
        loadPersistentFiles()
        loadPlans()
        subscribeToChanges()
        setupDoubleClickListener()
    }

    private fun loadPlans() {
        plansListModel.clear()
        aiderPlanService.getAiderPlans().forEach { plan ->
            plansListModel.addElement(plan)
        }
    }

    private class PlanListCellRenderer : JPanel(BorderLayout()), ListCellRenderer<AiderPlan> {
        private val executeIcon = AllIcons.Actions.Execute
        private var showExecuteButton = false
        private var executeButtonBounds = Rectangle()
        private val label = JLabel()
        private val statusIcon = JLabel()
        private val countLabel = JLabel()
        private val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))

        init {
            isOpaque = true
            val contentPanel = JPanel(BorderLayout(8, 0))
            contentPanel.isOpaque = false
            contentPanel.add(statusIcon, BorderLayout.WEST)
            contentPanel.add(label, BorderLayout.CENTER)
            contentPanel.add(countLabel, BorderLayout.EAST)
            
            add(contentPanel, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.EAST)
            border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        }

        override fun getListCellRendererComponent(
            list: JList<out AiderPlan>?,
            value: AiderPlan?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            // Set background colors based on selection state
            background = if (isSelected) list?.selectionBackground else list?.background
            label.background = background
            label.foreground = if (isSelected) list?.selectionForeground else list?.foreground
            
            if (value != null) {
                val planFile = value.files.firstOrNull()
                val fileName = planFile?.filePath?.let { File(it).nameWithoutExtension } ?: "Unknown Plan"
                label.text = fileName
                label.toolTipText = planFile?.filePath
                
                statusIcon.icon = if (value.isPlanComplete()) 
                    AllIcons.Actions.Commit 
                else 
                    AllIcons.General.BalloonInformation
                
                val openItems = value.openChecklistItems().size
                val totalItems = value.totalChecklistItems()
                countLabel.text = if (openItems > 0) "($openItems/$totalItems)" else "($totalItems/$totalItems)"
                countLabel.foreground = when {
                    openItems == 0 -> UIManager.getColor("Label.foreground")
                    openItems < totalItems/2 -> UIManager.getColor("Label.infoForeground")
                    else -> Color(255, 140, 0) // Orange for many open items
                }
            }
            
            buttonPanel.isVisible = showExecuteButton
            return this
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            if (showExecuteButton) {
                executeButtonBounds = Rectangle(
                    width - 24,
                    (height - 16) / 2,
                    16,
                    16
                )
                executeIcon.paintIcon(this, g, executeButtonBounds.x, executeButtonBounds.y)
            }
        }

        fun showExecuteButton(show: Boolean) {
            showExecuteButton = show
        }

        fun getExecuteButtonBounds(): Rectangle = executeButtonBounds
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
            llm = "",  // Will use default from settings
            additionalArgs = "",
            files = selectedPlan.files,
            lintCmd = "",
            projectPath = project.basePath ?: "",
            aiderMode = AiderMode.STRUCTURED
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

    private fun setupDoubleClickListener() {
        persistentFilesList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val index = persistentFilesList.locationToIndex(e.point)
                    if (index != -1) {
                        val fileData = persistentFilesListModel.getElementAt(index)
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                        virtualFile?.let {
                            FileEditorManager.getInstance(project).openFile(it, true)
                            doubleClickListener?.invoke()
                        }
                    }
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
                    scrollCell(plansList)
                        .align(com.intellij.ui.dsl.builder.Align.FILL)
                        .resizableColumn()
                }
                row {
                    button("Refresh Plans") { loadPlans() }
                }
            }.resizableRow()
        }
    }

    private fun addPersistentFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
        val files = FileChooser.chooseFiles(descriptor, project, null)
        val fileDataList = files.flatMap { file ->
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

