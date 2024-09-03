package de.andrena.aidershortcut.inputdialog

import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel

class ReadOnlyFilesView(private val allFiles: List<String>, private val persistentFiles: List<String>) :
    JPanel(BorderLayout()) {
    private val rootNode = CheckedTreeNode("Files")
    private val tree: CheckboxTree = CheckboxTree(
        object : CheckboxTree.CheckboxTreeCellRenderer() {
            override fun customizeRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                if (value is CheckedTreeNode && value.userObject is File) {
                    val file = value.userObject as File
                    textRenderer.append(file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        },
        rootNode
    )

    private val toggleReadOnlyButton = JButton("Toggle Read-Only Mode").apply {
        addActionListener { toggleReadOnlyMode() }
    }

    init {
        updateTree()

        val actionPanel = JPanel().apply {
            add(toggleReadOnlyButton)
        }

        add(actionPanel, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)
        preferredSize = Dimension(400, 300)
    }

    private fun updateTree() {
        rootNode.removeAllChildren()

        (allFiles + persistentFiles).distinct().forEach { filePath ->
            val file = File(filePath)
            val node = CheckedTreeNode(file)
            node.isChecked = filePath in persistentFiles
            rootNode.add(node)
        }

        (tree.model as DefaultTreeModel).reload()
    }

    private fun toggleReadOnlyMode() {
        // Implement the logic to toggle read-only mode
    }

    fun getPersistentFiles(): List<String> {
        val persistentFiles = mutableListOf<String>()
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChildAt(i) as CheckedTreeNode
            if (child.isChecked) {
                persistentFiles.add((child.userObject as File).absolutePath)
            }
        }
        return persistentFiles
    }
}
