package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.plans.AiderPlanService

@Service(Service.Level.PROJECT)
class DocumentationFinderService(private val project: Project) {

    fun findDocumentationFiles(virtualFiles: Array<VirtualFile>): List<FileData> {
        return virtualFiles.flatMap { findDocumentationForFile(it) }.distinct()
    }

    private fun findDocumentationForFile(file: VirtualFile): List<FileData> {
        var currentDir = file.parent
        val projectRootPath = project.basePath
        while (currentDir != null && currentDir.path.startsWith(projectRootPath ?: "") && 
               !currentDir.path.contains(AiderPlanService.AIDER_PLANS_FOLDER)) {
            val markdownFiles = currentDir.children
                ?.filter { it.extension?.lowercase() == "md" && !it.name.startsWith(".aider") }
                ?.map { FileData(it.path, false) }
                ?: emptyList()

            if (markdownFiles.isNotEmpty()) {
                return markdownFiles
            }
            currentDir = currentDir.parent
        }

        return emptyList()
    }
}
