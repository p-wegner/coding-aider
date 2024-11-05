package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData

@Service(Service.Level.PROJECT)
class DocumentationFinderService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): DocumentationFinderService = project.service()
    }

    fun findDocumentationFiles(virtualFiles: Array<VirtualFile>): List<FileData> {
        return virtualFiles.flatMap { findDocumentationForFile(it) }.distinct()
    }

    private fun findDocumentationForFile(file: VirtualFile): List<FileData> {
        val docs = mutableListOf<FileData>()
        var currentDir = file.parent

        while (currentDir != null) {
            val markdownFiles = currentDir.children
                ?.filter { it.extension?.lowercase() == "md" }
                ?.map { FileData(it.path, false) }
                ?: emptyList()

            docs.addAll(markdownFiles)
            currentDir = currentDir.parent
        }

        return docs
    }
}