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
            .filterNot { file -> persistentFiles.any { normalizePath(it.filePath) == normalizePath(file.filePath) } }
            .toMutableList()

        val settings = getInstance()
        val documentationFiles = if (settings.enableDocumentationLookup) {
            val documentationFinderService = project.service<DocumentationFinderService>()
            documentationFinderService.findDocumentationFiles(files)
        } else {
            emptyList()
        }
            .filterNot { docFile ->
                persistentFiles.any { normalizePath(it.filePath) == normalizePath(docFile.filePath) } ||
                        traversedFiles.any { normalizePath(it.filePath) == normalizePath(docFile.filePath) }
            }

        traversedFiles.addAll(persistentFiles)
        traversedFiles.addAll(documentationFiles)

        return traversedFiles.distinctBy { normalizePath(it.filePath) }
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')

}
