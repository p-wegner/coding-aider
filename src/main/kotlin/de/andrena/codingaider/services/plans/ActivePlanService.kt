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
class ActivePlanService(private val project: Project) {
    private var activePlan: AiderPlan? = null

    fun setActivePlan(plan: AiderPlan) {
        activePlan = plan
    }

    fun getActivePlan(): AiderPlan? = activePlan

    fun clearActivePlan() {
        activePlan = null
    }

    private fun AiderPlan.isPlanComplete(): Boolean {
        return checklist.all { item -> item.isComplete() }
    }

    private fun ChecklistItem.isComplete(): Boolean {
        return checked && children.all { it.isComplete() }
    }

    fun handlePlanCompletion(success: Boolean = true) {
        activePlan?.let { plan ->
            if (!success || plan.isPlanComplete()) {
                clearActivePlan()
            }
        }
    }

    fun handlePlanError() {
        clearActivePlan()
    }

    fun continuePlan() {
        activePlan?.let { plan ->
            executePlanContinuation(plan)
        }
    }

    private fun executePlanContinuation(selectedPlan: AiderPlan) {
        try {
            if (selectedPlan.isPlanComplete()) {
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

            setActivePlan(selectedPlan)
            IDEBasedExecutor(project, commandData) { success ->
                handlePlanCompletion(success)
            }.execute()

        } catch (e: Exception) {
            println("Error during plan continuation: ${e.message}")
            clearActivePlan()
            throw e
        }
    }
}
