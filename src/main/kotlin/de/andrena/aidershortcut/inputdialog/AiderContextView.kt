package de.andrena.aidershortcut.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IconManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tree.TreeVisitor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import de.andrena.aidershortcut.command.FileData
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class AiderContextView(
    private val project: Project,
    private val allFiles: List<FileData>
) : JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Files")
    private val tree: Tree = Tree(rootNode)
    private val persistentFileManager = PersistentFileManager(project.basePath ?: "")
    private var persistentFiles: List<FileData> = persistentFileManager.loadPersistentFiles()

    init {
        updateTree()

        tree.apply {
            isRootVisible = false
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
                    if (value is DefaultMutableTreeNode && value.userObject is FileData) {
                        val fileData = value.userObject as FileData
                        text = File(fileData.filePath).name
                        icon = when {
                            fileData.isReadOnly && isPersistent(fileData) -> IconManager.getInstance()
                                .createRowIcon(AllIcons.Nodes.DataSchema, AllIcons.Nodes.DataTables)
                            fileData.isReadOnly -> IconManager.getInstance().createRowIcon(AllIcons.Nodes.DataSchema)
                            isPersistent(fileData) -> IconManager.getInstance()
                                .createRowIcon(AllIcons.Actions.Edit, AllIcons.Nodes.DataTables)
                            else -> AllIcons.Actions.Edit
                        }
                        val tooltipText = buildString {
                            append(fileData.filePath)
                            if (fileData.isReadOnly) append(" (readonly)")
                            if (isPersistent(fileData)) append(" (persistent)")
                        }
                        toolTipText = tooltipText
                    }
                    background = null // Remove the grey background
                }
            }
        }

        val scrollPane = JBScrollPane(tree).apply {
            border = JBUI.Borders.empty()
        }
        add(scrollPane, BorderLayout.CENTER)
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
                }
            }
        })

        TreeUtil.expandAll(tree)
    }

    private fun updateTree() {
        val expandedPaths = getExpandedPaths()
        rootNode.removeAllChildren()

        val uniqueFiles = (allFiles + persistentFiles).distinctBy { it.filePath }
        val fileSystem = mutableMapOf<String, DefaultMutableTreeNode>()

        uniqueFiles.forEach { fileData ->
            val pathParts = fileData.filePath.split(File.separator)
            var currentPath = ""
            var currentNode = rootNode

            for ((index, part) in pathParts.withIndex()) {
                currentPath += if (currentPath.isEmpty()) part else File.separator + part
                val node = fileSystem.getOrPut(currentPath) {
                    val newNode = if (index == pathParts.lastIndex) {
                        DefaultMutableTreeNode(fileData)
                    } else {
                        DefaultMutableTreeNode(part)
                    }
                    currentNode.add(newNode)
                    newNode
                }
                currentNode = node
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

    fun toggleReadOnlyMode() {
        val selectedPaths = tree.selectionPaths ?: return
        val selectedNodes = selectedPaths.mapNotNull { it.lastPathComponent as? DefaultMutableTreeNode }

        selectedNodes.forEach { node ->
            if (node.userObject is FileData) {
                val fileData = node.userObject as FileData
                val updatedFileData = fileData.copy(isReadOnly = !fileData.isReadOnly)
                node.userObject = updatedFileData

                // Update persistentFiles
                persistentFiles = persistentFiles.map {
                    if (it.filePath == fileData.filePath) updatedFileData else it
                }
            }
        }

        (tree.model as DefaultTreeModel).reload()

        val selectionModel = tree.selectionModel
        selectedPaths.forEach { path ->
            tree.expandPath(path)
            selectionModel.addSelectionPath(path)
        }

        // Save updated persistent files
        persistentFileManager.savePersistentFilesToContextFile()
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
        selectedFiles.forEach { fileData ->
            if (isPersistent(fileData)) {
                persistentFileManager.removeFile(fileData.filePath)
            } else {
                persistentFileManager.addFile(fileData)
            }
        }
        persistentFiles = persistentFileManager.getPersistentFiles()
        updateTree()
    }

    private fun isPersistent(fileData: FileData): Boolean {
        return persistentFiles.any { it.filePath == fileData.filePath }
    }
}
