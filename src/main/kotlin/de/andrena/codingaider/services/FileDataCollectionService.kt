package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileTraversal

@Service(Service.Level.PROJECT)
class FileDataCollectionService(private val project: Project) {

    private val settings: AiderSettings = getInstance()

    fun collectAllFiles(
        files: Array<VirtualFile>,
        includePersistentFiles: Boolean = true

    ): List<FileData> {
        val traversedFiles = FileTraversal.traverseFilesOrDirectories(files).toMutableList()
        if (includePersistentFiles) {
            traversedFiles.addAll(project.service<PersistentFileService>().getPersistentFiles())
        }
        if (this.settings.enableDocumentationLookup) {
            traversedFiles.addAll(project.service<DocumentationFinderService>().findDocumentationFiles(files))
        }

        return traversedFiles.distinctBy { normalizePath(it.filePath) }
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')

}
