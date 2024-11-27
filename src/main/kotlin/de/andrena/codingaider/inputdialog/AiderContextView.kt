package de.andrena.codingaider.inputdialog

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
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
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER
import de.andrena.codingaider.services.FileExtractorService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.TokenCountService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class AiderContextView(
    private val project: Project,
    private var allFiles: List<FileData>,
    private val onFileNameSelected: (String) -> Unit,
    private val onFilesChanged: () -> Unit
) : JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Context")
    private val filesNode = DefaultMutableTreeNode("Files")
    private val markdownFilesNode = DefaultMutableTreeNode("Docs")
    private val planFilesNode = DefaultMutableTreeNode("Plan Files")
    private val tree: Tree = Tree(rootNode)
    private val persistentFileService = project.getService(PersistentFileService::class.java)
    private val tokenCountService = project.getService(TokenCountService::class.java)
    private var persistentFiles: List<FileData> = persistentFileService.loadPersistentFiles()

    init {
        rootNode.add(filesNode)
        rootNode.add(markdownFilesNode)
        rootNode.add(planFilesNode)
        selectedFilesChanged()
        tree.apply {
            isRootVisible = true
            showsRootHandles = true
            selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
            cellRenderer = object : DefaultTreeCellRenderer() {
                override fun getTreeCellRendererComponent(
                    tree: JTree,
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
                                val file =
                                    FileExtractorService.getInstance().extractFileIfNeeded(userObject)
                                val name = file?.let { File(it.filePath).nameWithoutExtension } ?: ""
                                icon = when {
                                    userObject.isReadOnly && isPersistent(userObject) -> IconManager.getInstance()
                                        .createRowIcon(AllIcons.Nodes.DataSchema, AllIcons.Nodes.DataTables)

                                    userObject.isReadOnly -> IconManager.getInstance()
                                        .createRowIcon(AllIcons.Nodes.DataSchema)

                                    isPersistent(userObject) -> IconManager.getInstance()
                                        .createRowIcon(AllIcons.Actions.Edit, AllIcons.Nodes.DataTables)

                                    else -> AllIcons.Actions.Edit
                                }
                                val fileContent = file?.let { File(it.filePath).readText() } ?: ""
                                val tokenCount = tokenCountService.countTokensInText(fileContent)
                                text = "$name (Tokens: $tokenCount)"
                                toolTipText = buildString {
                                    append(userObject.filePath)
                                    append(" (Tokens: $tokenCount)")
                                    if (userObject.isReadOnly) append(" (readonly)")
                                    if (isPersistent(userObject)) append(" (persistent)")
                                }
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

        val scrollPane = JBScrollPane(tree)
        add(scrollPane, BorderLayout.CENTER)
        preferredSize = JBUI.size(400, 200)

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

        if (AiderSettings.getInstance().alwaysIncludeOpenFiles) {
            addOpenFilesToContext()
        }
        if (AiderSettings.getInstance().alwaysIncludePlanContextFiles) {
            addPlanContextFilesToContext()
        }
        onFilesChanged()
    }

    fun selectedFilesChanged() {
        val expandedPaths = getExpandedPaths()
        filesNode.removeAllChildren()
        markdownFilesNode.removeAllChildren()
        planFilesNode.removeAllChildren()

        val allUniqueFiles = (allFiles + persistentFiles).distinctBy { it.filePath }

        allUniqueFiles.forEach { fileData ->
            val node = DefaultMutableTreeNode(fileData)
            when {
                fileData.filePath.contains(AiderPlanService.AIDER_PLANS_FOLDER) -> {
                    planFilesNode.add(node)
                }
                fileData.filePath.endsWith(".md") && fileData.filePath.contains(AIDER_DOCS_FOLDER) -> {
                    markdownFilesNode.add(node)
                }
                else -> {
                    filesNode.add(node)
                }
            }
        }

        (tree.model as DefaultTreeModel).reload()

        expandedPaths.forEach { path ->
            tree.expandPath(path)
        }

        TreeUtil.expandAll(tree)
        onFilesChanged()
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
        selectedFilesChanged()
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

        selectedFilesChanged()

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
        return FileExtractorService.getInstance().extractFilesIfNeeded(filesFromTree).distinctBy { it.filePath }
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
        selectedFilesChanged()
    }


    fun removeSelectedFiles() {
        val selectedFiles = getSelectedFiles()
        allFiles = allFiles.filterNot { it in selectedFiles }
        persistentFiles = persistentFiles.filterNot { it in selectedFiles }
        persistentFileService.removePersistentFiles(selectedFiles.map { it.filePath })
        selectedFilesChanged()
    }

    fun addFilesToContext(fileDataList: List<FileData>) = addFilesToTree(fileDataList)
    fun setFiles(files: List<FileData>) {
        allFiles = files
        selectedFilesChanged()
    }

    fun addPlanContextFilesToContext() {
        val contextFilesForPlans =
            project.service<AiderPlanService>().getContextFilesForPlans(getAllFiles())
        addFilesToContext(contextFilesForPlans)
    }

}
