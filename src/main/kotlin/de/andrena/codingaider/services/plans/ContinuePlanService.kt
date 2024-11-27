package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class ContinuePlanService(private val project: Project) {
    private var _currentRunningPlan: AiderPlan? = null

    fun continuePlan(selectedPlan: AiderPlan) {
        try {
            if (selectedPlan.isPlanComplete() || _currentRunningPlan == selectedPlan) {
                return
            }
            val fileSystem = LocalFileSystem.getInstance()
            val settings = AiderSettings.getInstance()

            val virtualFiles: List<VirtualFile> =
                selectedPlan.allFiles.mapNotNull {
                    fileSystem.findFileByPath(it.filePath) ?: fileSystem.findFileByPath(
                        project.basePath + "/" + it.filePath
                    )
                }

            if (virtualFiles.isEmpty()) {
                throw IllegalStateException("No valid files found for plan continuation")
            }

            val filesToInclude =
                project.service<FileDataCollectionService>().collectAllFiles(virtualFiles.toTypedArray())
            if (filesToInclude.isEmpty()) {
                throw IllegalStateException("No files collected for plan continuation")
            }

            val openItems = selectedPlan.openChecklistItems()
            val nextItem = openItems.firstOrNull()?.description
                ?: throw IllegalStateException("No open items found in checklist")

            val commandData = CommandData(
                message = "Continue implementing the plan. Next item: $nextItem",
                useYesFlag = settings.useYesFlag,
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = filesToInclude,
                lintCmd = settings.lintCmd,
                projectPath = project.basePath ?: throw IllegalStateException("Project base path not found"),
                aiderMode = AiderMode.STRUCTURED,
                sidecarMode = settings.useSidecarMode
            )
            // TODO: when to clear the field
            _currentRunningPlan = selectedPlan

            IDEBasedExecutor(project, commandData).execute()

        } catch (e: Exception) {
            println("Error during plan continuation: ${e.message}")
            throw e
        } finally {
        }
    }
}
