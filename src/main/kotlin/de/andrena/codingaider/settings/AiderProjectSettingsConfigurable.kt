package de.andrena.codingaider.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.features.documentation.DocumentTypeConfiguration
import de.andrena.codingaider.features.documentation.dialogs.DocumentTypeDialog
import de.andrena.codingaider.features.testgeneration.TestTypeConfiguration
import de.andrena.codingaider.features.testgeneration.dialogs.TestTypeDialog
import de.andrena.codingaider.features.customactions.CustomActionConfiguration
import de.andrena.codingaider.features.customactions.dialogs.CustomActionTypeDialog
import de.andrena.codingaider.services.PersistentFileService
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

class AiderProjectSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val persistentFilesListModel: DefaultListModel<FileData>
    private val persistentFilesList: JBList<FileData>
    private val persistentFileService: PersistentFileService
    private val plansFolderPathField = JBTextField()

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
        loadSettings()
        
        settingsComponent = panel {
            group("General Settings") {
                row("Plans Folder Path:") {
                    cell(plansFolderPathField)
                        .align(Align.FILL)
                        .resizableColumn()
                    button("Browse") {
                        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
                        descriptor.title = "Select Plans Folder"
                        descriptor.description = "Choose the folder where plans will be stored"
                        val selectedFile = FileChooser.chooseFile(descriptor, project, null)
                        selectedFile?.let {
                            val relativePath = project.basePath?.let { basePath ->
                                val projectFile = java.io.File(basePath)
                                val selectedFileObj = java.io.File(it.path)
                                try {
                                    projectFile.toPath().relativize(selectedFileObj.toPath()).toString().replace('\\', '/')
                                } catch (e: IllegalArgumentException) {
                                    it.path
                                }
                            } ?: it.path
                            plansFolderPathField.text = relativePath
                        }
                    }
                }
                row {
                    comment("Leave empty to use default (.coding-aider-plans). Path is relative to project root.")
                }
            }
            group("Test Types") {
                row {
                    scrollCell(createTestTypePanel())
                        .align(Align.FILL)
                        .resizableColumn()
                }
            }
            group("Document Types") {
                row {
                    scrollCell(createDocumentTypePanel())
                        .align(Align.FILL)
                        .resizableColumn()
                }
            }
            group("Custom Actions") {
                row {
                    scrollCell(createCustomActionPanel())
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

    private fun createDocumentTypePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val listModel = DefaultListModel<DocumentTypeConfiguration>()
        val documentTypeList = JBList(listModel)

        // Add existing document types
        AiderProjectSettings.getInstance(project).getDocumentTypes().forEach {
            listModel.addElement(it)
        }

        documentTypeList.cellRenderer = DocumentTypeRenderer()

        val buttonPanel = JPanel().apply {
            add(JButton("Add").apply {
                addActionListener {
                    showDocumentTypeDialog(null) { config ->
                        listModel.addElement(config)
                        AiderProjectSettings.getInstance(project).addDocumentType(config)
                    }
                }
            })
            add(JButton("Edit").apply {
                addActionListener {
                    val selected = documentTypeList.selectedValue
                    if (selected != null) {
                        val index = documentTypeList.selectedIndex
                        showDocumentTypeDialog(selected) { config ->
                            listModel.set(index, config)
                            AiderProjectSettings.getInstance(project).updateDocumentType(index, config)
                        }
                    }
                }
            })
            add(JButton("Remove").apply {
                addActionListener {
                    val index = documentTypeList.selectedIndex
                    if (index != -1) {
                        listModel.remove(index)
                        AiderProjectSettings.getInstance(project).removeDocumentType(index)
                    }
                }
            })
            add(JButton("Copy").apply {
                addActionListener {
                    val selected = documentTypeList.selectedValue
                    if (selected != null) {
                        val copy = selected.copy(name = "${selected.name} (Copy)")
                        listModel.addElement(copy)
                        AiderProjectSettings.getInstance(project).addDocumentType(copy)
                    }
                }
            })
        }

        panel.add(JScrollPane(documentTypeList), BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        return panel
    }

    private fun showTestTypeDialog(
        existing: TestTypeConfiguration?,
        onSave: (TestTypeConfiguration) -> Unit
    ) {
        // If we have an existing configuration with relative paths, convert to absolute for editing
        val configForEditing = existing?.withAbsolutePaths(project.basePath ?: "")

        val dialog = TestTypeDialog(project, configForEditing)
        if (dialog.showAndGet()) {
            onSave(dialog.getTestType())
        }
    }

    private fun showDocumentTypeDialog(
        existing: DocumentTypeConfiguration?,
        onSave: (DocumentTypeConfiguration) -> Unit
    ) {
        // If we have an existing configuration with relative paths, convert to absolute for editing
        val configForEditing = existing?.withAbsolutePaths(project.basePath ?: "")

        val dialog = DocumentTypeDialog(project, configForEditing)
        if (dialog.showAndGet()) {
            onSave(dialog.getDocumentType())
        }
    }

    private fun createCustomActionPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val listModel = DefaultListModel<CustomActionConfiguration>()
        val customActionList = JBList(listModel)

        // Add existing custom actions
        AiderProjectSettings.getInstance(project).getCustomActions().forEach {
            listModel.addElement(it)
        }

        customActionList.cellRenderer = CustomActionRenderer()

        val buttonPanel = JPanel().apply {
            add(JButton("Add").apply {
                addActionListener {
                    showCustomActionDialog(null) { config ->
                        listModel.addElement(config)
                        AiderProjectSettings.getInstance(project).addCustomAction(config)
                    }
                }
            })
            add(JButton("Edit").apply {
                addActionListener {
                    val selected = customActionList.selectedValue
                    if (selected != null) {
                        val index = customActionList.selectedIndex
                        showCustomActionDialog(selected) { config ->
                            listModel.set(index, config)
                            AiderProjectSettings.getInstance(project).updateCustomAction(index, config)
                        }
                    }
                }
            })
            add(JButton("Remove").apply {
                addActionListener {
                    val index = customActionList.selectedIndex
                    if (index != -1) {
                        listModel.remove(index)
                        AiderProjectSettings.getInstance(project).removeCustomAction(index)
                    }
                }
            })
            add(JButton("Copy").apply {
                addActionListener {
                    val selected = customActionList.selectedValue
                    if (selected != null) {
                        val copy = selected.copy(name = "${selected.name} (Copy)")
                        listModel.addElement(copy)
                        AiderProjectSettings.getInstance(project).addCustomAction(copy)
                    }
                }
            })
        }

        panel.add(JScrollPane(customActionList), BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        return panel
    }

    private fun showCustomActionDialog(
        existing: CustomActionConfiguration?,
        onSave: (CustomActionConfiguration) -> Unit
    ) {
        // If we have an existing configuration with relative paths, convert to absolute for editing
        val configForEditing = existing?.withAbsolutePaths(project.basePath ?: "")

        val dialog = CustomActionTypeDialog(project, configForEditing)
        if (dialog.showAndGet()) {
            onSave(dialog.getCustomAction())
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

    private inner class DocumentTypeRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is DocumentTypeConfiguration) {
                component.text = "${value.name} ${if (!value.isEnabled) "(Disabled)" else ""}"
            }
            return component
        }
    }

    private inner class CustomActionRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (component is JLabel && value is CustomActionConfiguration) {
                component.text = "${value.name} ${if (!value.isEnabled) "(Disabled)" else ""}"
            }
            return component
        }
    }

    override fun isModified(): Boolean {
        val settings = AiderProjectSettings.getInstance(project)
        return plansFolderPathField.text != (settings.plansFolderPath ?: "")
    }

    override fun apply() {
        val settings = AiderProjectSettings.getInstance(project)
        settings.plansFolderPath = plansFolderPathField.text.takeIf { it.isNotBlank() }
    }

    override fun reset() {
        loadPersistentFiles()
        loadSettings()
    }

    private fun loadSettings() {
        val settings = AiderProjectSettings.getInstance(project)
        plansFolderPathField.text = settings.plansFolderPath ?: ""
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
