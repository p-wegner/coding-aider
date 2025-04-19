package de.andrena.codingaider.toolwindow.persistentfiles

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.model.StashInfo
import de.andrena.codingaider.services.PersistentFileService
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.io.File
import javax.swing.*

class PersistentFilesPanel(private val project: Project) {
    private val persistentFileService = project.getService(PersistentFileService::class.java)
    private val persistentFilesListModel = DefaultListModel<FileData>()
    private val persistentFilesList: JBList<FileData> = JBList(persistentFilesListModel).apply {
        cellRenderer = PersistentFileRenderer()
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                    removeSelectedFiles()
                }
            }
        })
    }
    
    private val stashListModel = DefaultListModel<StashInfo>()
    private val stashList: JBList<StashInfo> = JBList(stashListModel).apply {
        cellRenderer = StashRenderer()
        addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount >= 2) {
                    val index = stashList.locationToIndex(e.point)
                    if (index >= 0) {
                        val cellBounds = stashList.getCellBounds(index, index)
                        if (cellBounds != null && cellBounds.contains(e.point)) {
                            val stashInfo = stashListModel.getElementAt(index)
                            openStashedFiles(stashInfo)
                        }
                    }
                }
            }
        })
    }

    init {
        loadPersistentFiles()
        loadStashes()
        subscribeToChanges()
        persistentFilesList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount >= 2) {
                    val index = persistentFilesList.locationToIndex(e.point)
                    if (index >= 0) {
                        val cellBounds = persistentFilesList.getCellBounds(index, index)
                        if (cellBounds != null && cellBounds.contains(e.point)) {
                            val fileData = persistentFilesListModel.getElementAt(index)
                            openFileInEditor(fileData)
                        }
                    }
                }
            }
        })
    }

    private fun subscribeToChanges() {
        project.messageBus.connect().subscribe(
            PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC,
            object : PersistentFilesChangedTopic {
                override fun onPersistentFilesChanged() {
                    loadPersistentFiles()
                    loadStashes()
                }
            }
        )
    }

    fun getContent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // Create files panel
        val filesPanel = panel {
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
                        add(object : 
                            AnAction("Stash Files", "Stash selected files", AllIcons.Vcs.ShelveSilent) {
                            override fun actionPerformed(e: AnActionEvent) = stashSelectedFiles()
                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = persistentFilesList.selectedIndices.isNotEmpty()
                            }
                            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
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
            }.resizableRow()
        }
        
        // Create stashes panel
        val stashesPanel = panel {
            row {
                val toolbar = ActionManager.getInstance().createActionToolbar(
                    "StashesToolbar",
                    DefaultActionGroup().apply {
                        add(object : 
                            AnAction("Pop Stash", "Restore files from stash and delete stash", AllIcons.Vcs.Unshelve) {
                            override fun actionPerformed(e: AnActionEvent) = popSelectedStash()
                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = stashList.selectedIndices.isNotEmpty()
                            }
                            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                        })
                        add(object : 
                            AnAction("Unstash", "Add stashed files without deleting stash", AllIcons.Vcs.Branch) {
                            override fun actionPerformed(e: AnActionEvent) = unstashSelectedStash()
                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = stashList.selectedIndices.isNotEmpty()
                            }
                            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                        })
                        add(object : 
                            AnAction("Delete Stash", "Delete selected stash", AllIcons.General.Remove) {
                            override fun actionPerformed(e: AnActionEvent) = deleteSelectedStash()
                            override fun update(e: AnActionEvent) {
                                e.presentation.isEnabled = stashList.selectedIndices.isNotEmpty()
                            }
                            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                        })
                    },
                    true
                )
                toolbar.targetComponent = stashList
                cell(Wrapper(toolbar.component))
            }
            row {
                scrollCell(stashList)
                    .align(Align.FILL)
                    .resizableColumn()
            }.resizableRow()
        }
        
        // Create splitter with files and stashes
        val splitter = JBSplitter(true, 0.7f)
        splitter.firstComponent = filesPanel
        splitter.secondComponent = stashesPanel
        splitter.dividerWidth = 3
        
        mainPanel.add(splitter, BorderLayout.CENTER)
        return mainPanel
    }

    private fun addPersistentFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true).apply {
            withFileFilter { file -> !persistentFileService.isIgnored(file.path) }
        }
        val files = FileChooser.chooseFiles(descriptor, project, null)
        
        val fileDataList = files.flatMap { file ->
            if (file.isDirectory) {
                file.children
                    .filter { it.isValid && !it.isDirectory && !persistentFileService.isIgnored(it.path) }
                    .map { FileData(it.path, false) }
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
    
    private fun stashSelectedFiles() {
        val selectedFiles = persistentFilesList.selectedValuesList
        if (selectedFiles.isEmpty()) return
        
        val stashName = Messages.showInputDialog(
            project,
            "Enter a name for this stash (optional):",
            "Stash Files",
            Messages.getQuestionIcon()
        ) ?: return
        
        try {
            persistentFileService.stashFiles(selectedFiles, stashName)
            loadStashes()
        } catch (e: Exception) {
            Messages.showErrorDialog(
                "Failed to stash files: ${e.message}",
                "Stash Error"
            )
        }
    }
    
    private fun popSelectedStash() {
        val selectedStash = stashList.selectedValue ?: return
        
        val result = Messages.showYesNoDialog(
            "Are you sure you want to restore files from stash '${selectedStash.getDisplayName()}'?\nThis will delete the stash after restoring.",
            "Pop Stash",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            persistentFileService.popStash(selectedStash)
            loadStashes()
            loadPersistentFiles()
        }
    }
    
    private fun unstashSelectedStash() {
        val selectedStash = stashList.selectedValue ?: return
        
        val result = Messages.showYesNoDialog(
            "Add files from stash '${selectedStash.getDisplayName()}' to persistent files?\nThe stash will be kept.",
            "Unstash Files",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            persistentFileService.unstashFiles(selectedStash)
            loadPersistentFiles()
        }
    }
    
    private fun deleteSelectedStash() {
        val selectedStash = stashList.selectedValue ?: return
        
        val result = Messages.showYesNoDialog(
            "Are you sure you want to delete stash '${selectedStash.getDisplayName()}'?",
            "Delete Stash",
            Messages.getQuestionIcon()
        )
        
        if (result == Messages.YES) {
            persistentFileService.deleteStash(selectedStash)
            loadStashes()
        }
    }
    
    private fun openStashedFiles(stashInfo: StashInfo) {
        val stashFile = File(project.basePath ?: "", stashInfo.getFileName())
        if (!stashFile.exists()) {
            Messages.showErrorDialog(
                "Stash file not found: ${stashInfo.getFileName()}",
                "Error Opening Stash"
            )
            return
        }
        
        try {
            val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(stashFile)
            virtualFile?.let { FileEditorManager.getInstance(project).openFile(it, true) }
        } catch (e: Exception) {
            Messages.showErrorDialog(
                "Error opening stash file: ${e.message}",
                "Stash Error"
            )
        }
    }

    private fun openFileInEditor(fileData: FileData) {
        val file = File(fileData.filePath)
        if (file.exists()) {
            val virtualFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            virtualFile?.let { FileEditorManager.getInstance(project).openFile(it, true) }
        }
    }

    private fun loadPersistentFiles() {
        persistentFilesListModel.clear()
        persistentFileService.getPersistentFiles().forEach { file ->
            persistentFilesListModel.addElement(file)
        }
    }
    
    private fun loadStashes() {
        stashListModel.clear()
        persistentFileService.getStashes().forEach { stash ->
            stashListModel.addElement(stash)
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
                component.icon = AllIcons.FileTypes.Text
            }
            return component
        }
    }
    
    private inner class StashRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is StashInfo) {
                component.text = "${value.getDisplayName()} (${value.fileCount} files)"
                component.icon = AllIcons.Vcs.ShelveSilent
            }
            return component
        }
    }
}
