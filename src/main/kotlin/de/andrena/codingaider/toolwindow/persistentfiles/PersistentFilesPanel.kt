package de.andrena.codingaider.toolwindow.persistentfiles

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.services.PersistentFileService
import java.awt.Component
import java.io.File
import javax.swing.*

class PersistentFilesPanel(private val project: Project) {
    private val persistentFileService = project.getService(PersistentFileService::class.java)
    private val persistentFilesListModel = DefaultListModel<FileData>()
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
        subscribeToChanges()
        persistentFilesList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val index = persistentFilesList.locationToIndex(e.point)
                    if (index >= 0) {
                        val fileData = persistentFilesListModel.getElementAt(index)
                        openFileInEditor(fileData)
                    }
                }
            }
        })

    }

    private fun subscribeToChanges() {
        project.messageBus.connect().subscribe(
            PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC,
            object : PersistentFilesChangedTopic {
                override fun onPersistentFilesChanged() = loadPersistentFiles()
            }
        )
    }

    fun getContent(): JComponent {
        return panel {
            row {
                val toolbar = ActionManager.getInstance().createActionToolbar(
                    "PersistentFilesToolbar",
                    DefaultActionGroup().apply {
                        add(object : AnAction("Add Files", "Add files to persistent list", AllIcons.General.Add) {
                            override fun actionPerformed(e: AnActionEvent) = addPersistentFiles()
                        })
                        add(object :
                            AnAction("Add Open Files", "Add currently open files", AllIcons.Actions.OpenNewTab) {
                            override fun actionPerformed(e: AnActionEvent) = addOpenFilesToPersistent()
                        })
                        add(object :
                            AnAction("Toggle Read-Only", "Toggle read-only status", AllIcons.Actions.Edit) {
                            override fun actionPerformed(e: AnActionEvent) = toggleReadOnlyMode()
                        })
                        add(object : AnAction("Remove Files", "Remove selected files", AllIcons.General.Remove) {
                            override fun actionPerformed(e: AnActionEvent) = removeSelectedFiles()
                        })
                    },
                    true
                )
                toolbar.targetComponent = persistentFilesList
                cell(Wrapper(toolbar.component))
            }
            row {
                scrollCell(persistentFilesList)
                    .align(Align.FILL)
                    .resizableColumn()
            }
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

    private fun openFileInEditor(fileData: FileData) {
        val file = File(fileData.filePath)
        if (file.exists()) {
            val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByIoFile(file)
            virtualFile?.let { FileEditorManager.getInstance(project).openFile(it, true) }
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
