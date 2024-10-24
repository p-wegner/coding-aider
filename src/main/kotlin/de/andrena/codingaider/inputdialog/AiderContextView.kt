package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IconManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class AiderContextView(
    private val project: Project,
    private var allFiles: List<FileData>,
    private val onFileNameSelected: (String) -> Unit
) : JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Context")
    private val filesNode = DefaultMutableTreeNode("Files")
    private val markdownFilesNode = DefaultMutableTreeNode("Docs")
    private val tree: Tree = Tree(rootNode)
    private val persistentFileService = project.getService(PersistentFileService::class.java)
    private var persistentFiles: List<FileData> = persistentFileService.loadPersistentFiles()

    init {
        rootNode.add(filesNode)
        rootNode.add(markdownFilesNode)
        updateTree()
        tree.apply {
            isRootVisible = true
            showsRootHandles = true
            selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
            cellRenderer = object : DefaultTreeCellRenderer() {
                override fun getTreeCellRendererComponent(
                    tree: javax.swing.JTree,
                    value: Any?,
                    sel: Boolean,
                    expanded: Boolean,
                    leaf: Boolean,
                    row: Int,
                    hasFocus: Boolean
                ) = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus).apply {
                    if (value is DefaultMutableTreeNode) {
                        when (val userObject = value.userObject) {
                            is FileData -> {
                                text = File(userObject.filePath).name
                                icon = when {
                                    userObject.isReadOnly && isPersistent(userObject) -> IconManager.getInstance()
                                        .createRowIcon(AllIcons.Nodes.DataSchema, AllIcons.Nodes.DataTables)

                                    userObject.isReadOnly -> IconManager.getInstance()
                                        .createRowIcon(AllIcons.Nodes.DataSchema)

                                    isPersistent(userObject) -> IconManager.getInstance()
                                        .createRowIcon(AllIcons.Actions.Edit, AllIcons.Nodes.DataTables)

                                    else -> AllIcons.Actions.Edit
                                }
                                val tooltipText = buildString {
                                    append(userObject.filePath)
                                    if (userObject.isReadOnly) append(" (readonly)")
                                    if (isPersistent(userObject)) append(" (persistent)")
                                }
                                toolTipText = tooltipText
                            }

                            "Context" -> icon = AllIcons.Nodes.Module
                            "Files" -> icon = AllIcons.Nodes.Folder
                        }
                    }
                    background = if (sel) JBColor.background().brighter() else JBColor.background()
                    foreground = JBColor.foreground()
                }
            }
        }

        val scrollPane = JBScrollPane(tree).apply {
            border = JBUI.Borders.empty(10)
        }

        val contentPanel = JPanel(BorderLayout()).apply {
            add(scrollPane, BorderLayout.CENTER)
            border = TitledBorder(
                JBUI.Borders.customLine(JBColor.border(), 1),
                "Context",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                null,
                JBColor.foreground()
            )
        }

        add(contentPanel, BorderLayout.CENTER)
        preferredSize = JBUI.size(400, 300)

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
                    if (node?.userObject is FileData) {
                        val fileData = node.userObject as FileData
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                        virtualFile?.let {
                            FileEditorManager.getInstance(project).openFile(it, true)
                        }
                    }
                } else if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown) {
                    val node = tree.getPathForLocation(e.x, e.y)?.lastPathComponent as? DefaultMutableTreeNode
                    if (node?.userObject is FileData) {
                        val fileData = node.userObject as FileData
                        val fileName = File(fileData.filePath).nameWithoutExtension
                        onFileNameSelected(fileName)
                    }
                }
            }
        })

        val inputMap = tree.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = tree.actionMap

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeFiles")
        actionMap.put("removeFiles", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                removeSelectedFiles()
            }
        })

        TreeUtil.expandAll(tree)

        fun getOpenFiles(): List<FileData> {
            val openFiles = FileEditorManager.getInstance(project).openFiles
            return openFiles.map { virtualFile ->
                FileData(virtualFile.path, false)
            }
        }
        if (AiderSettings.getInstance().alwaysIncludeOpenFiles) {
            addOpenFilesToContext()
        }
    }

    fun updateTree() {
        val expandedPaths = getExpandedPaths()
        filesNode.removeAllChildren()
        markdownFilesNode.removeAllChildren()

        val allUniqueFiles = (allFiles + persistentFiles).distinctBy { it.filePath }

        allUniqueFiles.forEach { fileData ->
            val node = DefaultMutableTreeNode(fileData)
            if (fileData.filePath.endsWith(".md") && fileData.filePath.contains(".aider-docs")) {
                markdownFilesNode.add(node)
            } else {
                filesNode.add(node)
            }
        }

        (tree.model as DefaultTreeModel).reload()

        expandedPaths.forEach { path ->
            tree.expandPath(path)
        }

        TreeUtil.expandAll(tree)
    }

    private fun getExpandedPaths(): List<javax.swing.tree.TreePath> {
        val paths = mutableListOf<javax.swing.tree.TreePath>()
        val root = tree.model.root as DefaultMutableTreeNode
        val e = root.depthFirstEnumeration()
        while (e.hasMoreElements()) {
            val node = e.nextElement() as DefaultMutableTreeNode
            val path = javax.swing.tree.TreePath(node.path)
            if (tree.isExpanded(path)) {
                paths.add(path)
            }
        }
        return paths
    }

    fun getOpenFiles(): List<FileData> {
        val openFiles = FileEditorManager.getInstance(project).openFiles
        return openFiles.map { virtualFile ->
            FileData(virtualFile.path, false)
        }
    }

    fun addOpenFilesToContext() {
        val openFiles = getOpenFiles()
        val newFiles = openFiles.filter { openFile ->
            allFiles.none { it.filePath == openFile.filePath }
        }
        allFiles += newFiles
        updateTree()
    }

    fun toggleReadOnlyMode() {
        val selectedPaths = tree.selectionPaths ?: return
        val selectedNodes = selectedPaths.mapNotNull { it.lastPathComponent as? DefaultMutableTreeNode }

        selectedNodes.forEach { node ->
            if (node.userObject is FileData) {
                val fileData = node.userObject as FileData
                val updatedFileData = fileData.copy(isReadOnly = !fileData.isReadOnly)
                node.userObject = updatedFileData

                // Update allFiles
                allFiles = allFiles.map {
                    if (it.filePath == fileData.filePath) updatedFileData else it
                }

                // Update persistentFiles
                persistentFiles = persistentFiles.map {
                    if (it.filePath == fileData.filePath) updatedFileData else it
                }

                // Update the file in persistentFileManager
                if (isPersistent(updatedFileData)) {
                    persistentFileService.updateFile(updatedFileData)
                }
            }
        }

        updateTree()

        // Restore selection
        val selectionModel = tree.selectionModel
        selectedPaths.forEach { path ->
            val newPath = findUpdatedPath(path)
            tree.expandPath(newPath)
            selectionModel.addSelectionPath(newPath)
        }

        // Save updated persistent files
        persistentFileService.savePersistentFilesToContextFile()
    }

    private fun findUpdatedPath(oldPath: javax.swing.tree.TreePath): javax.swing.tree.TreePath {
        val components = oldPath.path
        val updatedComponents = components.map { component ->
            if (component is DefaultMutableTreeNode && component.userObject is FileData) {
                val fileData = component.userObject as FileData
                val updatedFileData = allFiles.find { it.filePath == fileData.filePath } ?: fileData
                DefaultMutableTreeNode(updatedFileData)
            } else {
                component
            }
        }.toTypedArray()
        return javax.swing.tree.TreePath(updatedComponents)
    }

    fun getSelectedFiles(): List<FileData> {
        return tree.selectionPaths?.mapNotNull { path ->
            (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? FileData
        } ?: emptyList()
    }

    fun getAllFiles(): List<FileData> {
        val filesFromTree = mutableListOf<FileData>()
        val root = tree.model.root as DefaultMutableTreeNode
        val e = root.depthFirstEnumeration()

        while (e.hasMoreElements()) {
            val node = e.nextElement() as DefaultMutableTreeNode
            if (node.userObject is FileData) {
                val fileData = node.userObject as FileData
                filesFromTree.add(fileData)
            }
        }
        return filesFromTree.distinctBy { it.filePath }
    }

    fun togglePersistentFile() {
        val selectedFiles = getSelectedFiles()
        val selectedPaths = tree.selectionPaths ?: return

        selectedFiles.forEach { fileData ->
            if (isPersistent(fileData)) {
                persistentFileService.removeFile(fileData.filePath)
            } else {
                persistentFileService.addFile(fileData)
            }
        }
        persistentFiles = persistentFileService.getPersistentFiles()
        addFilesToTree(persistentFiles)

        // Restore selection
        tree.selectionModel.clearSelection()
        selectedPaths.forEach { path ->
            val newPath = findUpdatedPath(path)
            tree.expandPath(newPath)
            tree.addSelectionPath(newPath)
        }
    }

    private fun isPersistent(fileData: FileData): Boolean {
        return persistentFiles.any { it.filePath == fileData.filePath }
    }

    private fun addFilesToTree(files: List<FileData>) {
        files.forEach { file ->
            if (!allFiles.any { it.filePath == file.filePath }) {
                allFiles += file
            }
        }
        updateTree()
    }


    fun removeSelectedFiles() {
        val selectedFiles = getSelectedFiles()
        allFiles = allFiles.filterNot { it in selectedFiles }
        persistentFiles = persistentFiles.filterNot { it in selectedFiles }
        persistentFileService.removePersistentFiles(selectedFiles.map { it.filePath })
        updateTree()
    }

    fun addFilesToContext(fileDataList: List<FileData>) = addFilesToTree(fileDataList)
    fun setFiles(files: List<FileData>) {
        allFiles = files
        updateTree()
    }

}
