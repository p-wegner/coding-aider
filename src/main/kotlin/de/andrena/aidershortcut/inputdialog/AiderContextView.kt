package de.andrena.aidershortcut.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
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
                    icon =
                        if (file.absolutePath in persistentFiles) AllIcons.Actions.Edit else AllIcons.General.InspectionsOK
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
        rootNode.removeAllChildren()

        (allFiles + persistentFiles).distinct().forEach { filePath ->
            val file = File(filePath)
            val node = DefaultMutableTreeNode(file)
            rootNode.add(node)
        }

        (tree.model as DefaultTreeModel).reload()
    }

    fun toggleReadOnlyMode() {
        val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
        if (selectedNode != null && selectedNode.userObject is File) {
            val file = selectedNode.userObject as File
            persistentFiles = if (file.absolutePath in persistentFiles) {
                persistentFiles - file.absolutePath
            } else {
                persistentFiles + file.absolutePath
            }
            updateTree()
        }
    }

    fun getPersistentFiles(): List<String> = persistentFiles
}
