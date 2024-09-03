package de.andrena.aidershortcut.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.IconManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
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
    private val allFiles: List<String>,
    private var persistentFiles: List<String>
) :
    JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Files")
    private val tree: Tree = Tree(rootNode)

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
                if (value is DefaultMutableTreeNode && value.userObject is File) {
                    val file = value.userObject as File
                    text = file.name
                    icon = if (file.absolutePath in persistentFiles) {
                        IconManager.getInstance().createRowIcon(AllIcons.Nodes.DataSchema)
                    } else {
                        AllIcons.Actions.Edit
                    }
                    val isReadOnly = file.absolutePath in persistentFiles
                    val tooltipText = file.absolutePath + (if (isReadOnly) " (readonly)" else "")
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
                    if (node?.userObject is File) {
                        val file = node.userObject as File
                        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
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

        val uniqueFiles = (allFiles + persistentFiles).distinct().map { File(it) }

        uniqueFiles.forEach { file ->
            if (!rootNode.children().asSequence().any { (it as DefaultMutableTreeNode).userObject == file }) {
                val node = DefaultMutableTreeNode(file)
                rootNode.add(node)
            }
        }

        (tree.model as DefaultTreeModel).reload()
        
        // Restore expanded state
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
            if (node.userObject is File) {
                val file = node.userObject as File
                persistentFiles = if (file.absolutePath in persistentFiles) {
                    persistentFiles - file.absolutePath
                } else {
                    persistentFiles + file.absolutePath
                }
            }
        }
        
        updateTree()
        
        // Restore selection and expand paths
        val selectionModel = tree.selectionModel
        selectedPaths.forEach { path ->
            tree.expandPath(path)
            selectionModel.addSelectionPath(path)
        }
    }

    fun getPersistentFiles(): List<String> = persistentFiles

    fun addToPersistentFiles(filePath: String) {
        if (filePath !in persistentFiles) {
            persistentFiles = persistentFiles + filePath
            updateTree()
        }
    }

    fun getSelectedFiles(): List<File> {
        return tree.selectionPaths?.mapNotNull { path ->
            (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? File
        } ?: emptyList()
    }
}
