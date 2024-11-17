package de.andrena.codingaider.services.plans

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class ContinuePlanService(private val project: Project) {
    fun continuePlan(selectedPlan: AiderPlan) {
        val settings = AiderSettings.getInstance()
        val virtualFiles: List<VirtualFile> =
            selectedPlan.allFiles.mapNotNull { VirtualFileManager.getInstance().findFileByUrl(it.filePath) }
        val filesToInclude = project.service<FileDataCollectionService>().collectAllFiles(virtualFiles.toTypedArray())

        val commandData = CommandData(
            message = "",
            useYesFlag = settings.useYesFlag,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs,
            files = filesToInclude,
            lintCmd = settings.lintCmd,
            projectPath = project.basePath ?: "",
            aiderMode = AiderMode.STRUCTURED,
            sidecarMode = settings.useSidecarMode
        )

        IDEBasedExecutor(project, commandData).execute()
    }
}
