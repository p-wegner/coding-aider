package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileTraversal
import de.andrena.codingaider.utils.ScratchFileUtils
import fleet.tracing.meter

@Service(Service.Level.PROJECT)
class FileDataCollectionService(private val project: Project) {

    private val settings: AiderSettings = getInstance()

    private val aiderIgnoreService = project.service<AiderIgnoreService>()
    fun collectAllFiles(
        files: Array<VirtualFile> = emptyArray(),
        includePersistentFiles: Boolean = true
    ): List<FileData> {
        val traversedFiles = FileTraversal.traverseFilesOrDirectories(files)
            .filterNot { fileData ->
                // Allow scratch files even if they match aiderignore patterns
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileData.filePath)
                val isScratchFile = virtualFile?.let { ScratchFileUtils.isScratchFile(it) } 
                    ?: ScratchFileUtils.isScratchFileByPath(fileData.filePath)
                
                !isScratchFile && aiderIgnoreService.isIgnored(fileData.filePath)
            }
            .toMutableList()
            
        if (includePersistentFiles) {
            traversedFiles.addAll(project.service<PersistentFileService>().getPersistentFiles())
        }
        if (this.settings.enableDocumentationLookup) {
            traversedFiles.addAll(project.service<DocumentationFinderService>().findDocumentationFiles(files))
        }
        // TODO 26.08.2025 pwegner: expand context yaml files if setting is on
        return traversedFiles.distinctBy { normalizePath(it.filePath) }
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/').replace("//", "/")

}
