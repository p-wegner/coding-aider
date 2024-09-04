package de.andrena.aidershortcut.inputdialog

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.aidershortcut.command.FileData
import java.io.File
import java.io.FileWriter
import java.io.IOException

class PersistentFileManager(private val basePath: String) {
    private val contextFile = File(basePath, ".aider.context")
    private val persistentFiles: MutableList<FileData> = mutableListOf()

    init {
        loadPersistentFiles()
    }

    fun loadPersistentFiles(): List<FileData> {
        if (contextFile.exists()) {
            try {
                persistentFiles.clear()
                contextFile.readLines().forEach { line ->
                    persistentFiles.add(FileData(line.trim(), true))
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
                persistentFiles.forEach { file ->
                    writer.write("${file.filePath}\n")
                }
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
