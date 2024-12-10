package de.andrena.codingaider.toolwindow.plans

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import java.io.File
import javax.swing.*

class EditContextDialog(
    private val project: Project,
    private val plan: AiderPlan
) : DialogWrapper(project) {
    private val contextFilesListModel = DefaultListModel<FileData>()
    private val contextFilesList = JBList(contextFilesListModel).apply {
        cellRenderer = PlanViewer.PlanListCellRenderer(true)
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                    removeSelectedFiles()
                }
            }
        })
    }

    init {
        title = "Edit Context Files"
        init()
        loadContextFiles()
    }

    private fun loadContextFiles() {
        contextFilesListModel.clear()
        plan.contextFiles.forEach { file ->
            contextFilesListModel.addElement(file)
        }
    }

    private fun addContextFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
        val files = FileChooser.chooseFiles(descriptor, project, null)
        val fileDataList = files.flatMap { file ->
            if (file.isDirectory) {
                file.children.filter { it.isValid && !it.isDirectory }.map { FileData(it.path, false) }
            } else {
                listOf(FileData(file.path, false))
            }
        }
        fileDataList.forEach { file ->
            if (!contextFilesListModel.contains(file)) {
                contextFilesListModel.addElement(file)
            }
        }
        saveContextFiles()
    }

    private fun toggleReadOnlyMode() {
        val selectedFiles = contextFilesList.selectedValuesList
        selectedFiles.forEach { fileData ->
            val index = contextFilesListModel.indexOf(fileData)
            if (index != -1) {
                contextFilesListModel.set(index, fileData.copy(isReadOnly = !fileData.isReadOnly))
            }
        }
        saveContextFiles()
    }

    private fun removeSelectedFiles() {
        val selectedFiles = contextFilesList.selectedValuesList
        selectedFiles.forEach { file ->
            contextFilesListModel.removeElement(file)
        }
        saveContextFiles()
    }

    private fun saveContextFiles() {
        val contextFile = File(plan.contextYamlFile?.filePath ?: return)
        val yamlContent = buildString {
            appendLine("---")
            appendLine("files:")
            val files = mutableListOf<FileData>()
            for (i in 0 until contextFilesListModel.size()) {
                files.add(contextFilesListModel.getElementAt(i))
            }
            files.forEach { file ->
                appendLine("- path: \"${file.filePath}\"")
                appendLine("  readOnly: ${file.isReadOnly}")
            }
        }
        contextFile.writeText(yamlContent)
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            group("Context Files") {
                row {
                    val toolbar = ActionManager.getInstance().createActionToolbar(
                        "ContextFilesToolbar",
                        DefaultActionGroup().apply {
                            add(object : AnAction("Add Files", "Add files to context", AllIcons.General.Add) {
                                override fun actionPerformed(e: AnActionEvent) = addContextFiles()
                            })
                            add(object : AnAction("Toggle Read-Only", "Toggle read-only status", AllIcons.Actions.Edit) {
                                override fun actionPerformed(e: AnActionEvent) = toggleReadOnlyMode()
                            })
                            add(object : AnAction("Remove Files", "Remove selected files", AllIcons.General.Remove) {
                                override fun actionPerformed(e: AnActionEvent) = removeSelectedFiles()
                            })
                        },
                        true
                    )
                    toolbar.targetComponent = contextFilesList
                    cell(Wrapper(toolbar.component))
                }
                row {
                    scrollCell(contextFilesList)
                        .align(Align.FILL)
                        .resizableColumn()
                }
            }.resizableRow()
        }
    }
}
