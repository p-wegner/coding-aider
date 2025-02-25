package de.andrena.codingaider.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.dialogs.TestTypeDialog
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.testgeneration.TestTypeConfiguration
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class AiderProjectSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val persistentFilesListModel: DefaultListModel<FileData>
    private val persistentFilesList: JBList<FileData>
    private val persistentFileService: PersistentFileService

    init {
        this.persistentFileService = project.getService(PersistentFileService::class.java)
        this.persistentFilesListModel = DefaultListModel<FileData>()
        this.persistentFilesList = JBList(persistentFilesListModel).apply {
            addKeyListener(object : java.awt.event.KeyAdapter() {
                override fun keyPressed(e: java.awt.event.KeyEvent) {
                    if (e.keyCode == java.awt.event.KeyEvent.VK_DELETE) {
                        removeSelectedFiles()
                    }
                }
            })
        }
    }

    override fun getDisplayName(): String = "Aider Project Settings"

    override fun createComponent(): JComponent {
        persistentFilesList.cellRenderer = PersistentFileRenderer()
        loadPersistentFiles()
        settingsComponent = panel {
            group("Test Types") {
                row {
                    scrollCell(createTestTypePanel())
                        .align(Align.FILL)
                        .resizableColumn()
                }
            }
            group("Persistent Files") {
                row {
                    scrollCell(persistentFilesList)
                        .align(Align.FILL)
                        .resizableColumn()
                }
                row {
                    button("Add Files") { addPersistentFiles() }
                    button("Toggle Read-Only") { toggleReadOnlyMode() }
                    button("Remove Files") { removeSelectedFiles() }
                }
            }
        }.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }

        return settingsComponent!!
    }

    private fun createTestTypePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val listModel = DefaultListModel<TestTypeConfiguration>()
        val testTypeList = JBList(listModel)

        // Add existing test types
        AiderProjectSettings.getInstance(project).getTestTypes().forEach {
            listModel.addElement(it)
        }

        testTypeList.cellRenderer = TestTypeRenderer()

        val buttonPanel = JPanel().apply {
            add(JButton("Add").apply {
                addActionListener {
                    showTestTypeDialog(null) { config ->
                        listModel.addElement(config)
                        AiderProjectSettings.getInstance(project).addTestType(config)
                    }
                }
            })
            add(JButton("Edit").apply {
                addActionListener {
                    val selected = testTypeList.selectedValue
                    if (selected != null) {
                        val index = testTypeList.selectedIndex
                        showTestTypeDialog(selected) { config ->
                            listModel.set(index, config)
                            AiderProjectSettings.getInstance(project).updateTestType(index, config)
                        }
                    }
                }
            })
            add(JButton("Remove").apply {
                addActionListener {
                    val index = testTypeList.selectedIndex
                    if (index != -1) {
                        listModel.remove(index)
                        AiderProjectSettings.getInstance(project).removeTestType(index)
                    }
                }
            })
            add(JButton("Copy").apply {
                addActionListener {
                    val selected = testTypeList.selectedValue
                    if (selected != null) {
                        val copy = selected.copy(name = "${selected.name} (Copy)")
                        listModel.addElement(copy)
                        AiderProjectSettings.getInstance(project).addTestType(copy)
                    }
                }
            })
        }

        panel.add(JScrollPane(testTypeList), BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        return panel
    }

    private fun showTestTypeDialog(
        existing: TestTypeConfiguration?,
        onSave: (TestTypeConfiguration) -> Unit
    ) {
        val dialog = TestTypeDialog(project, existing)
        if (dialog.showAndGet()) {
            onSave(dialog.getTestType())
        }
    }

    private inner class TestTypeRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is TestTypeConfiguration) {
                component.text = "${value.name} ${if (!value.isEnabled) "(Disabled)" else ""}"
            }
            return component
        }
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
    }

    override fun reset() {
        loadPersistentFiles()
    }

    private fun addPersistentFiles() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
        val files = FileChooser.chooseFiles(descriptor, project, null)
        val fileDataList = files.flatMap { file ->
            if (file.isDirectory) {
                file.children.filter { it.isValid && !it.isDirectory }.map { FileData(it.path, false) }
            } else {
                listOf(FileData(file.path, false))
            }
        }
        persistentFileService.addAllFiles(fileDataList)
        loadPersistentFiles()
    }

    private fun toggleReadOnlyMode() {
        val selectedFiles = persistentFilesList.selectedValuesList
        selectedFiles.forEach { fileData ->
            val updatedFileData = fileData.copy(isReadOnly = !fileData.isReadOnly)
            persistentFileService.updateFile(updatedFileData)
        }
        loadPersistentFiles()
    }

    private fun removeSelectedFiles() {
        val selectedFiles = persistentFilesList.selectedValuesList
        persistentFileService.removePersistentFiles(selectedFiles.map { it.filePath })
        loadPersistentFiles()
    }

    private fun loadPersistentFiles() {
        persistentFilesListModel.clear()
        persistentFileService.getPersistentFiles().forEach { file ->
            persistentFilesListModel.addElement(file)
        }
    }


    private inner class PersistentFileRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is FileData) {
                component.text = "${value.filePath} ${if (value.isReadOnly) "(Read-Only)" else ""}"
            }
            return component
        }
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}
