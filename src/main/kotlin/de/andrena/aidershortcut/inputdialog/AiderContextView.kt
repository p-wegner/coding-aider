package de.andrena.aidershortcut.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IconManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
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

        tree.cellRenderer = object : DefaultTreeCellRenderer() {
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
                        fileData.isReadOnly && isPersistent(fileData) -> IconManager.getInstance().createRowIcon(AllIcons.Nodes.DataSchema, AllIcons.Nodes.PinToolWindow)
                        fileData.isReadOnly -> IconManager.getInstance().createRowIcon(AllIcons.Nodes.DataSchema)
                        isPersistent(fileData) -> IconManager.getInstance().createRowIcon(AllIcons.Actions.Edit, AllIcons.Nodes.PinToolWindow)
                        else -> AllIcons.Actions.Edit
                    }
                    val tooltipText = fileData.filePath + 
                        (if (fileData.isReadOnly) " (readonly)" else "") + 
                        (if (isPersistent(fileData)) " (persistent)" else "")
                    toolTipText = tooltipText
                }
            }
        }

        add(JBScrollPane(tree), BorderLayout.CENTER)
        preferredSize = Dimension(400, 300)

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
    }

    private fun updateTree() {
        val expandedPaths = getExpandedPaths()

        rootNode.removeAllChildren()

        val uniqueFiles = (allFiles + persistentFiles).distinctBy { it.filePath }

        uniqueFiles.forEach { fileData ->
            if (!rootNode.children().asSequence().any { (it as DefaultMutableTreeNode).userObject == fileData }) {
                val node = DefaultMutableTreeNode(fileData)
                rootNode.add(node)
            }
        }

        (tree.model as DefaultTreeModel).reload()

        expandedPaths.forEach { path ->
            tree.expandPath(path)
        }
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
