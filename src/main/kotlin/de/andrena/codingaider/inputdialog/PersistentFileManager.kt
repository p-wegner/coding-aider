package de.andrena.codingaider.inputdialog

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import java.io.File
import java.io.IOException

class PersistentFileManager(private val project: Project) {
    private val contextFile = File(project.basePath ?:"", ".aider.context.yaml")
    private val persistentFiles: MutableList<FileData> = mutableListOf()
    private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    init {
        loadPersistentFiles()
    }

    private fun notifyPersistentFilesChanged() {
        ApplicationManager.getApplication().invokeLater {
            project.messageBus.syncPublisher(PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC).onPersistentFilesChanged()
        }
    }

    fun loadPersistentFiles(): List<FileData> {
        if (contextFile.exists()) {
            try {
                persistentFiles.clear()
                val yamlData: Map<String, List<Map<String, Any>>> = objectMapper.readValue(contextFile)
                yamlData["files"]?.forEach { fileMap ->
                    val filePath = fileMap["path"] as? String
                    val isReadOnly = fileMap["readOnly"] as? Boolean ?: false
                    if (filePath != null) {
                        persistentFiles.add(FileData(filePath, isReadOnly))
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return persistentFiles
    }

    fun savePersistentFilesToContextFile() {
        try {
            val data = mapOf(
                "files" to persistentFiles.map { file ->
                    mapOf(
                        "path" to file.filePath,
                        "readOnly" to file.isReadOnly
                    )
                }
            )
            objectMapper.writeValue(contextFile, data)
            refreshContextFile()
            notifyPersistentFilesChanged()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun refreshContextFile() {
        ApplicationManager.getApplication().invokeLater {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contextFile)?.refresh(false, false)
        }
    }

    fun addFile(file: FileData) {
        if (!persistentFiles.any { it.filePath == file.filePath }) {
            persistentFiles.add(file)
            savePersistentFilesToContextFile()
        }
    }

    fun removeFile(filePath: String) {
        persistentFiles.removeIf { it.filePath == filePath }
        savePersistentFilesToContextFile()
    }

    fun getPersistentFiles(): List<FileData> = persistentFiles
    fun addAllFiles(selectedFiles: List<FileData>) {
        selectedFiles.forEach { addFile(it) }
    }

    fun updateFile(updatedFile: FileData) {
        val index = persistentFiles.indexOfFirst { it.filePath == updatedFile.filePath }
        if (index != -1) {
            persistentFiles[index] = updatedFile
            savePersistentFilesToContextFile()
        }
    }

    fun getContextFile(): VirtualFile {
        if (!contextFile.exists()) {
            contextFile.createNewFile()
            savePersistentFilesToContextFile()
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(contextFile)!!
    }

    fun removePersistentFiles(filePaths: List<String>) {
        persistentFiles.removeAll { it.filePath in filePaths }
        savePersistentFilesToContextFile()
    }
}
