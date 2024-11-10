package de.andrena.codingaider.toolwindow.persistentfiles

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
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
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedFile = persistentFilesList.selectedValue
                    selectedFile?.let { openFileInEditor(it) }
                }
            }
        })
    }

    init {
        loadPersistentFiles()
        subscribeToChanges()
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
            group("Persistent Files") {
                row {
                    scrollCell(persistentFilesList)
                        .align(Align.FILL)
                        .resizableColumn()
                }
                row {
                    button("Add Files") { addPersistentFiles() }
                    button("Add Open Files") { addOpenFilesToPersistent() }
                    button("Toggle Read-Only") { toggleReadOnlyMode() }
                    button("Remove Files") { removeSelectedFiles() }
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
