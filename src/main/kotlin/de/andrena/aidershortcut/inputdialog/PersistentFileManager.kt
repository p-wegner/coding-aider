package de.andrena.aidershortcut.inputdialog

import java.io.File

class PersistentFileManager(private val basePath: String) {
    private val persistentFiles: MutableList<String> = mutableListOf()

    fun loadPersistentFiles(): List<String> {
        // Logic to load persistent files from storage (e.g., a file or database)
        // For now, we will return the existing list
        return persistentFiles
    }

    fun savePersistentFiles(files: List<String>) {
        // Logic to save persistent files to storage
        persistentFiles.clear()
        persistentFiles.addAll(files)
    }

    fun addFile(filePath: String) {
        if (filePath !in persistentFiles) {
            persistentFiles.add(filePath)
        }
    }

    fun removeFile(filePath: String) {
        persistentFiles.remove(filePath)
    }

    fun getPersistentFiles(): List<String> = persistentFiles
}
