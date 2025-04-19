package de.andrena.codingaider.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.messages.PersistentFilesChangedTopic
import de.andrena.codingaider.model.ContextFileHandler
import de.andrena.codingaider.model.StashInfo
import de.andrena.codingaider.model.StashManager
import de.andrena.codingaider.utils.FileTraversal
import java.io.File
import java.io.IOException

@Service(Service.Level.PROJECT)
class PersistentFileService(private val project: Project) {
    private val contextFile = File(project.basePath ?: "", ".aider.context.yaml")
    private val persistentFiles: MutableList<FileData> = mutableListOf()
    private val filesChanged: PersistentFilesChangedTopic by lazy {
        project.messageBus.syncPublisher(PersistentFilesChangedTopic.PERSISTENT_FILES_CHANGED_TOPIC)
    }
    private val aiderIgnoreService by lazy { project.service<AiderIgnoreService>() }

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
        if (!persistentFiles.any { it.hasSameNormalizedPath(file)} && !isIgnored(file.filePath)) {
            persistentFiles.add(file)
            savePersistentFilesToContextFile()
        }
    }


    fun removeFile(filePath: String) {
        val normalizedPath = FileTraversal.normalizedFilePath(filePath)
        persistentFiles.removeIf { FileTraversal.normalizedFilePath(it.filePath) == normalizedPath }
        savePersistentFilesToContextFile()
    }

    fun getPersistentFiles(): List<FileData> {
        // Return current list immediately while we check validity in background
        ApplicationManager.getApplication().executeOnPooledThread {
            // Clean up persistent files that no longer exist or are now ignored
            val validFiles = persistentFiles.filter { fileData ->
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                (virtualFile?.exists() ?: false) && !isIgnored(fileData.filePath)
            }

            if (validFiles.size != persistentFiles.size) {
                persistentFiles.clear()
                persistentFiles.addAll(validFiles)
                savePersistentFilesToContextFile()
            }
        }
        return persistentFiles
    }

    fun addAllFiles(selectedFiles: List<FileData>) {
        val nonIgnoredFiles = selectedFiles.filterNot { isIgnored(it.filePath) }
        nonIgnoredFiles.forEach { addFile(it) }
    }

    fun updateFile(updatedFile: FileData) {
        val index = persistentFiles.indexOfFirst { it.filePath == updatedFile.filePath }
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(updatedFile.filePath)

        if (index != -1 && virtualFile?.exists() == true && !isIgnored(updatedFile.filePath)) {
            persistentFiles[index] = updatedFile
            savePersistentFilesToContextFile()
        } else if (index != -1) {
            // If file no longer exists or is now ignored, remove it from persistent files
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
    
    fun isIgnored(filePath: String): Boolean {
        return aiderIgnoreService.isIgnored(filePath)
    }
    
    // Stash-related functions
    fun stashFiles(selectedFiles: List<FileData>, stashName: String = ""): StashInfo {
        val stashInfo = StashInfo(name = stashName, fileCount = selectedFiles.size)
        val stashFile = File(project.basePath ?: "", stashInfo.getFileName())
        
        try {
            ContextFileHandler.writeContextFile(stashFile, selectedFiles, project.basePath ?: "")
            
            // Remove stashed files from persistent files
            val filePaths = selectedFiles.map { it.filePath }
            removePersistentFiles(filePaths)
            
            // Refresh files in the file system
            ApplicationManager.getApplication().invokeLater {
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(stashFile)
                notifyPersistentFilesChanged()
            }
            
            return stashInfo
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    fun getStashes(): List<StashInfo> {
        return StashManager.getStashFiles(project).map { StashManager.getStashInfo(it) }
    }
    
    fun popStash(stashInfo: StashInfo) {
        val stashFile = File(project.basePath ?: "", stashInfo.getFileName())
        if (!stashFile.exists()) return
        
        try {
            val stashedFiles = ContextFileHandler.readContextFile(stashFile, project.basePath ?: "")
            
            // Add stashed files back to persistent files
            addAllFiles(stashedFiles)
            
            // Delete the stash file
            stashFile.delete()
            
            // Refresh files in the file system
            ApplicationManager.getApplication().invokeLater {
                LocalFileSystem.getInstance().refresh(true)
                notifyPersistentFilesChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun deleteStash(stashInfo: StashInfo) {
        val stashFile = File(project.basePath ?: "", stashInfo.getFileName())
        if (stashFile.exists()) {
            stashFile.delete()
            ApplicationManager.getApplication().invokeLater {
                LocalFileSystem.getInstance().refresh(true)
                notifyPersistentFilesChanged()
            }
        }
    }
}
