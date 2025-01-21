package de.andrena.codingaider.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.model.ContextFileHandler
import java.io.File
import java.io.IOException

@Service(Service.Level.PROJECT)
class PersistentFileService(private val project: Project) {
    private val contextFile = File(project.basePath ?: "", ".aider.context.yaml")
    private val persistentFiles: MutableList<FileData> = mutableListOf()
    private val filesChanged: PersistentFilesChangedTopic by lazy {
        project.messageBus.syncPublisher(PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC)
    }

    init {
        loadPersistentFiles()
    }

    private fun notifyPersistentFilesChanged() {
        ApplicationManager.getApplication().invokeLater {
            filesChanged.onPersistentFilesChanged()
        }
    }

    fun loadPersistentFiles(): List<FileData> {
        if (contextFile.exists()) {
            try {
                persistentFiles.clear()
                persistentFiles.addAll(ContextFileHandler.readContextFile(contextFile, project.basePath ?: ""))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return persistentFiles
    }

    fun savePersistentFilesToContextFile() {
        try {
            ContextFileHandler.writeContextFile(contextFile, persistentFiles, project.basePath ?: "")
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
        if (!persistentFiles.any { it.hasSameNormalizedPath(file)}) {
            persistentFiles.add(file)
            savePersistentFilesToContextFile()
        }
    }

    fun removeFile(filePath: String) {
        persistentFiles.removeIf { it.filePath == filePath }
        savePersistentFilesToContextFile()
    }

    fun getPersistentFiles(): List<FileData> {
        // Clean up persistent files that no longer exist
        val validFiles = persistentFiles.filter { fileData ->
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
            virtualFile?.exists() ?: false
        }

        if (validFiles.size != persistentFiles.size) {
            persistentFiles.clear()
            persistentFiles.addAll(validFiles)
            savePersistentFilesToContextFile()
        }

        return persistentFiles
    }

    fun addAllFiles(selectedFiles: List<FileData>) {
        selectedFiles.forEach { addFile(it) }
    }

    fun updateFile(updatedFile: FileData) {
        val index = persistentFiles.indexOfFirst { it.filePath == updatedFile.filePath }
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(updatedFile.filePath)

        if (index != -1 && virtualFile?.exists() == true) {
            persistentFiles[index] = updatedFile
            savePersistentFilesToContextFile()
        } else if (index != -1) {
            // If file no longer exists, remove it from persistent files
            persistentFiles.removeAt(index)
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
