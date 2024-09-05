package de.andrena.codingaider.inputdialog

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
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
                val yamlData = yaml.load<Map<String, Any>>(contextFile.reader())
                (yamlData["files"] as? List<Map<String, Any>>)?.forEach { fileMap ->
                    val filePath = fileMap["path"] as? String
                    val isReadOnly = fileMap["readOnly"] as? Boolean ?: true
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
        } catch (e: IOException) {
            e.printStackTrace()
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

    fun getContextFile(): VirtualFile = LocalFileSystem.getInstance().findFileByIoFile(contextFile)!!
}
