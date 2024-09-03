package de.andrena.aidershortcut.inputdialog

import java.io.File
import java.io.FileWriter
import java.io.IOException

class PersistentFileManager(private val basePath: String) {
    private val contextFile = File(basePath, ".aider.context")
    private val persistentFiles: MutableList<String> = mutableListOf()

    init {
        loadPersistentFiles()
    }

    fun loadPersistentFiles(): List<String> {
        if (contextFile.exists()) {
            try {
                persistentFiles.clear()
                contextFile.readLines().forEach { line ->
                    persistentFiles.add(line.trim())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return persistentFiles
    }

    fun savePersistentFiles(files: List<String>) {
        persistentFiles.clear()
        persistentFiles.addAll(files)
        try {
            FileWriter(contextFile).use { writer ->
                persistentFiles.forEach { file ->
                    writer.write("$file\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun addFile(filePath: String) {
        if (filePath !in persistentFiles) {
            persistentFiles.add(filePath)
            savePersistentFiles(persistentFiles)
        }
    }

    fun removeFile(filePath: String) {
        persistentFiles.remove(filePath)
        savePersistentFiles(persistentFiles)
    }

    fun getPersistentFiles(): List<String> = persistentFiles
}
