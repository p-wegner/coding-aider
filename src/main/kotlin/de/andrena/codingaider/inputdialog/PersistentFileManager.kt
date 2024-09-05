package de.andrena.codingaider.inputdialog

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter
import java.io.IOException

class PersistentFileManager(basePath: String) {
    private val contextFile = File(basePath, ".aider.context.yaml")
    private val persistentFiles: MutableList<FileData> = mutableListOf()
    private val yaml: Yaml

    init {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.isPrettyFlow = true
        yaml = Yaml(options)
        loadPersistentFiles()
    }

    fun loadPersistentFiles(): List<FileData> {
        if (contextFile.exists()) {
            try {
                persistentFiles.clear()
                val yamlData: Map<String, Any> = yaml.load(contextFile.reader())
                (yamlData["files"] as? List<Map<String, Any>>)?.forEach { fileMap ->
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
            FileWriter(contextFile).use { writer ->
                val data = mapOf(
                    "files" to persistentFiles.map { file ->
                        mapOf(
                            "path" to file.filePath,
                            "readOnly" to file.isReadOnly
                        )
                    }
                )
                yaml.dump(data, writer)
            }
            refreshContextFile()
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
}
