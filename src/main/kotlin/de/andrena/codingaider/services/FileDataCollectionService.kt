package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileTraversal

@Service(Service.Level.PROJECT)
class FileDataCollectionService(private val project: Project) {
    fun collectAllFiles(
        files: Array<VirtualFile>
    ): List<FileData> {
        val persistentFileService = project.service<PersistentFileService>()
        val persistentFiles = persistentFileService.getPersistentFiles()
        val traversedFiles = FileTraversal.traverseFilesOrDirectories(files)
            .filterNot { file -> persistentFiles.any { it.filePath == file.filePath } }
            .toMutableList()

        val settings = getInstance()
        val documentationFiles = if (settings.enableDocumentationLookup) {
            val documentationFinderService = project.service<DocumentationFinderService>()
            documentationFinderService.findDocumentationFiles(files)
        } else {
            emptyList()
        }
            .filterNot { docFile ->
                persistentFiles.any { it.filePath == docFile.filePath } ||
                        traversedFiles.any { it.filePath == docFile.filePath }
            }

        traversedFiles.addAll(persistentFiles)
        traversedFiles.addAll(documentationFiles)

        val allFiles = traversedFiles.distinctBy { it.filePath }
        return allFiles
    }

}