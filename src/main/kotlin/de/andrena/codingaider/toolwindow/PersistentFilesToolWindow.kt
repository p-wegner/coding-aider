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
    private val planViewer = PlanViewer(project)
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



    init {
        loadPersistentFiles()
        loadPlans()
        subscribeToChanges()
        subscribeToFileChanges()
    }

    private fun loadPlans() {
        planViewer.updatePlans(aiderPlanService.getAiderPlans())
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
                        DefaultActionGroup().apply { add(planViewer.ContinuePlanAction()) },
                        true
                    )
                    toolbar.targetComponent = planViewer.plansList
                    cell(Wrapper(toolbar.component))
                }
                row {
                    scrollCell(planViewer.plansList)
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

