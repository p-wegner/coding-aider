package de.andrena.codingaider.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import de.andrena.codingaider.command.FileData
import javax.swing.*

class AiderProjectSettingsConfigurable(private val project: Project) : Configurable {
    private var settingsComponent: JPanel? = null
    private val persistentFilesListModel = DefaultListModel<FileData>()
    private val persistentFilesList = JBList(persistentFilesListModel)

    override fun getDisplayName(): String = "Aider Project Settings"

    override fun createComponent(): JComponent {
        persistentFilesList.cellRenderer = PersistentFileRenderer()
        loadPersistentFiles()
        settingsComponent = panel {
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

    override fun isModified(): Boolean {
        val settings = AiderProjectSettings.getInstance(project)
        return persistentFilesListModel.elements().toList() != settings.persistentFiles
    }

    override fun apply() {
        val settings = AiderProjectSettings.getInstance(project)
        settings.persistentFiles = persistentFilesListModel.elements().toList()
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
        persistentFilesListModel.addAll(fileDataList)
    }

    private fun toggleReadOnlyMode() {
        val selectedFiles = persistentFilesList.selectedValuesList
        selectedFiles.forEach { fileData ->
            val index = persistentFilesListModel.indexOf(fileData)
            persistentFilesListModel.set(index, fileData.copy(isReadOnly = !fileData.isReadOnly))
        }
    }

    private fun removeSelectedFiles() {
        val selectedFiles = persistentFilesList.selectedValuesList
        selectedFiles.forEach { persistentFilesListModel.removeElement(it) }
    }

    private fun loadPersistentFiles() {
        persistentFilesListModel.clear()
        val settings = AiderProjectSettings.getInstance(project)
        settings.persistentFiles.forEach { file ->
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
