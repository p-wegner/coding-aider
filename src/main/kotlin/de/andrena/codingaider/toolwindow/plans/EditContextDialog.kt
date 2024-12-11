package de.andrena.codingaider.toolwindow.plans

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.model.ContextFileHandler
import de.andrena.codingaider.model.ContextYamlData
import de.andrena.codingaider.model.ContextYamlFile
import de.andrena.codingaider.services.plans.AiderPlan
import de.andrena.codingaider.services.plans.AiderPlanService
import java.awt.Component
import java.io.File
import javax.swing.*

class EditContextDialog(
    private val project: Project,
    private val plan: AiderPlan
) : DialogWrapper(project) {
    private val persistentFileService = project.getService(PersistentFileService::class.java)
    private val contextFilesListModel = DefaultListModel<FileData>()
    private val contextFilesList = JBList(contextFilesListModel).apply {
        cellRenderer = ContextFileRenderer()
        addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                    removeSelectedFiles()
                }
            }
        })
        preferredSize = java.awt.Dimension(500, 300)
    }

    init {
        title = "Edit Context Files"
        init()
        loadContextFiles()
        setSize(600, 400)
    }

    private fun loadContextFiles() {
        contextFilesListModel.clear()

        // Load from context file if it exists
        val contextFile = File(plan.contextYamlFile?.filePath ?: return)
        if (contextFile.exists()) {
            val files = ContextFileHandler.readContextFile(contextFile, project.basePath ?: "")
            files.forEach { fileData ->
                if (File(fileData.filePath).exists()) {
                    contextFilesListModel.addElement(fileData)
                }
            }
        } else {
            // Use plan's context files if context file doesn't exist
            plan.contextFiles.forEach { file ->
                contextFilesListModel.addElement(file)
            }
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

    private fun addOpenFilesToContext() {
        val openFiles = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFiles
        openFiles.forEach { file ->
            val fileData = FileData(file.path, false)
            if (!contextFilesListModel.contains(fileData)) {
                contextFilesListModel.addElement(fileData)
            }
        }
        saveContextFiles()
    }

    private fun addPersistentFilesToContext() {
        persistentFileService.getPersistentFiles().forEach { fileData ->
            if (!contextFilesListModel.contains(fileData)) {
                contextFilesListModel.addElement(fileData)
            }
        }
        saveContextFiles()
    }

    private fun saveContextFiles() {
        val contextFile = File(plan.contextYamlFile?.filePath ?: return)
        val files = mutableListOf<ContextYamlFile>()
        for (i in 0 until contextFilesListModel.size()) {
            val file = contextFilesListModel.getElementAt(i)
            if (File(file.filePath).exists()) {
                files.add(ContextYamlFile(file.filePath, file.isReadOnly))
            }
        }
        val contextData = ContextYamlData(files)
        val yamlMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
        yamlMapper.writeValue(contextFile, contextData)

        // Ensure parent directory exists
        contextFile.parentFile?.mkdirs()

        // Refresh plan viewer and update plan service
        VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
        project.getService(AiderPlanService::class.java).getAiderPlans()
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
                            add(object : AnAction("Add Open Files", "Add currently open files", AllIcons.Actions.OpenNewTab) {
                                override fun actionPerformed(e: AnActionEvent) = addOpenFilesToContext()
                            })
                            add(object : AnAction("Add Persistent Files", "Add persistent files", AllIcons.Vcs.History) {
                                override fun actionPerformed(e: AnActionEvent) = addPersistentFilesToContext()
                            })
                            add(object : AnAction("Add Open Files", "Add currently open files", AllIcons.Actions.OpenNewTab) {
                                override fun actionPerformed(e: AnActionEvent) = addOpenFilesToContext()
                            })
                            add(object : AnAction("Add Persistent Files", "Add persistent files", AllIcons.Vcs.History) {
                                override fun actionPerformed(e: AnActionEvent) = addPersistentFilesToContext() 
                            })
                            add(object :
                                AnAction("Toggle Read-Only", "Toggle read-only status", AllIcons.Actions.Edit) {
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
                }.resizableRow()
            }.resizableRow()
        }
    }

    private inner class ContextFileRenderer : DefaultListCellRenderer() {
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
            }
            return component
        }
    }

}
