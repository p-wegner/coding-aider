package de.andrena.codingaider.features.documentation.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.services.GitRepoCloneService
import java.awt.Dimension
import java.io.File
import javax.swing.JButton
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel

class FileSelectionPanel(private val project: Project) {
    private val gitService = project.service<GitRepoCloneService>()
    
    private val fileTree = CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree?,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            if (value is CheckedTreeNode) {
                val userObject = value.userObject
                if (userObject is File) {
                    textRenderer.append(userObject.name)
                    if (userObject.isDirectory) {
                        textRenderer.icon = AllIcons.Nodes.Folder
                    } else {
                        textRenderer.icon = AllIcons.FileTypes.Text
                    }
                } else {
                    textRenderer.append(userObject?.toString() ?: "")
                }
            }
        }
    }, CheckedTreeNode("No repository cloned")).apply {
        isRootVisible = true
        showsRootHandles = true
    }
    
    private val selectAllButton = JButton("Select All").apply {
        addActionListener { selectAllFiles(true) }
    }
    
    private val deselectAllButton = JButton("Deselect All").apply {
        addActionListener { selectAllFiles(false) }
    }
    
    private val fileTypeFilterField = JBTextField().apply {
        emptyText.text = "Filter by file extensions (e.g., .kt,.java,.md)"
        addActionListener { applyFileTypeFilter() }
    }
    
    private val applyFilterButton = JButton("Apply Filter").apply {
        addActionListener { applyFileTypeFilter() }
    }
    
    fun createPanel() = panel {
        group("File Selection") {
            row {
                label("Select files/folders:")
            }
            row {
                cell(selectAllButton)
                    .align(AlignX.LEFT)
                cell(deselectAllButton)
                    .align(AlignX.LEFT)
            }
            row {
                label("File type filter:")
            }
            row {
                cell(fileTypeFilterField)
                    .resizableColumn()
                    .align(AlignX.FILL)
                cell(applyFilterButton)
                    .align(AlignX.RIGHT)
            }
            row {
                cell(JBScrollPane(fileTree))
                    .resizableColumn()
                    .align(AlignY.FILL)
                    .align(AlignX.FILL)
            }.resizableRow()
        }
    }
    
    fun updateFileTree(repoPath: String) {
        val root = CheckedTreeNode(File(repoPath).name)
        root.userObject = File(repoPath)

        val repoDir = File(repoPath)
        if (repoDir.exists()) {
            addDirectoryToTree(root, repoDir)
        }

        fileTree.model = DefaultTreeModel(root)
        fileTree.expandRow(0)
    }
    
    fun updateFileTreeFromVirtualFiles(files: Array<VirtualFile>, repoPath: String) {
        if (files.isNotEmpty()) {
            val root = CheckedTreeNode(File(repoPath).name)
            root.userObject = File(repoPath)

            val repoDir = File(repoPath)
            if (repoDir.exists()) {
                addDirectoryToTree(root, repoDir)
                // Pre-select the provided files
                preselectFiles(root, files, repoPath)
            }

            fileTree.model = DefaultTreeModel(root)
            fileTree.expandRow(0)

            // Expand some levels to show the structure
            for (i in 0 until minOf(3, fileTree.rowCount)) {
                fileTree.expandRow(i)
            }
        }
    }
    
    private fun addDirectoryToTree(parentNode: CheckedTreeNode, directory: File) {
        directory.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name })?.forEach { file ->
            if (!file.name.startsWith(".")) { // Skip hidden files like .git
                val node = CheckedTreeNode(file)
                parentNode.add(node)

                if (file.isDirectory) {
                    addDirectoryToTree(node, file)
                }
            }
        }
    }
    
    private fun selectAllFiles(selected: Boolean) {
        val root = fileTree.model.root as? CheckedTreeNode ?: return
        setNodeSelection(root, selected)
        fileTree.repaint()
    }
    
    private fun setNodeSelection(node: CheckedTreeNode, selected: Boolean) {
        node.isChecked = selected
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                setNodeSelection(child, selected)
            }
        }
    }
    
    private fun applyFileTypeFilter() {
        val filterText = fileTypeFilterField.text.trim()
        if (filterText.isEmpty()) {
            selectAllFiles(true)
            return
        }

        val extensions = filterText.split(",").map { it.trim().lowercase() }
        val root = fileTree.model.root as? CheckedTreeNode ?: return

        applyFilterToNode(root, extensions)
        fileTree.repaint()
    }
    
    private fun applyFilterToNode(node: CheckedTreeNode, extensions: List<String>): Boolean {
        val file = node.userObject as? File
        var hasMatchingChildren = false

        // First, check all children
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                val childMatches = applyFilterToNode(child, extensions)
                hasMatchingChildren = hasMatchingChildren || childMatches
            }
        }

        // For files, check if extension matches
        val fileMatches = if (file != null && file.isFile) {
            val fileExtension = file.extension.lowercase()
            extensions.any { ext ->
                val cleanExt = ext.removePrefix(".")
                fileExtension == cleanExt || ext == ".$fileExtension"
            }
        } else {
            false
        }

        // Select node if it's a matching file or has matching children
        val shouldSelect = fileMatches || hasMatchingChildren
        node.isChecked = shouldSelect

        return shouldSelect
    }
    
    private fun preselectFiles(root: CheckedTreeNode, files: Array<VirtualFile>, repoPath: String) {
        val filePaths = files.map {
            // Convert VirtualFile path to relative path from repo root
            val repoFile = File(repoPath)
            val virtualPath = File(it.path)
            try {
                repoFile.toPath().relativize(virtualPath.toPath()).toString().replace('\\', '/')
            } catch (e: Exception) {
                // Fallback to just the file name if relativization fails
                it.name
            }
        }.toSet()
        preselectNodesRecursively(root, filePaths, repoPath)
    }
    
    private fun preselectNodesRecursively(node: CheckedTreeNode, filePaths: Set<String>, repoPath: String) {
        val file = node.userObject as? File
        if (file != null) {
            val relativePath = try {
                File(repoPath).toPath().relativize(file.toPath()).toString().replace('\\', '/')
            } catch (e: Exception) {
                file.name
            }

            // Check if this file/directory should be selected
            val shouldSelect = filePaths.any { targetPath ->
                relativePath == targetPath ||
                        relativePath.endsWith(targetPath) ||
                        targetPath.endsWith(relativePath) ||
                        file.name == targetPath
            }

            if (shouldSelect) {
                node.isChecked = true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                preselectNodesRecursively(child, filePaths, repoPath)
            }
        }
    }
    
    fun getSelectedFiles(repoPath: String): Array<VirtualFile> {
        val virtualFiles = mutableListOf<VirtualFile>()

        val repoRoot = gitService.getRepositoryRoot(repoPath)
        if (repoRoot != null) {
            val root = fileTree.model.root as? CheckedTreeNode
            if (root != null) {
                collectCheckedFiles(root, repoPath, repoRoot, virtualFiles)
            }
        }

        return virtualFiles.toTypedArray()
    }
    
    private fun collectCheckedFiles(
        node: CheckedTreeNode,
        repoPath: String,
        repoRoot: VirtualFile,
        virtualFiles: MutableList<VirtualFile>
    ) {
        if (node.isChecked) {
            val file = node.userObject as? File
            if (file != null && file != File(repoPath)) { // Don't include the root directory itself
                val relativePath = File(repoPath).toPath().relativize(file.toPath()).toString().replace('\\', '/')
                val virtualFile = repoRoot.findFileByRelativePath(relativePath)
                if (virtualFile != null) {
                    virtualFiles.add(virtualFile)
                }
            }
        }

        // Always check children, even if parent is not checked (for partial selections)
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as? CheckedTreeNode
            if (child != null) {
                collectCheckedFiles(child, repoPath, repoRoot, virtualFiles)
            }
        }
    }
}
