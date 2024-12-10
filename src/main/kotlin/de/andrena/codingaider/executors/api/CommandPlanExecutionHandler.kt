package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.plans.AiderPlanService
import java.io.File


class CommandPlanExecutionHandler(private val project: Project, private val commandData: CommandData) {
    private var initialPlanFiles: Set<File> = emptySet()

    fun commandCompleted() {
        if (!commandData.structuredMode) return
        val plansFolder = File(project.basePath, AiderPlanService.AIDER_PLANS_FOLDER)
        if (plansFolder.exists() && plansFolder.isDirectory) {
            val currentPlanFiles = plansFolder.listFiles { file -> file.isFile && file.extension == "md" }
                ?.toSet() ?: emptySet()

            val newPlanFiles = currentPlanFiles.subtract(initialPlanFiles)
            newPlanFiles.forEach { file ->
                project.service<PersistentFileService>().addFile(FileData(file.absolutePath, false))
            }

            // Set active plan when new plan files are created
            if (newPlanFiles.isNotEmpty()) {
                val planService = project.service<AiderPlanService>()
                val plans = planService.getAiderPlans()
                // Find the newest plan based on the new files
                val newestPlan = plans.firstOrNull { plan ->
                    plan.mainPlanFile?.let { mainFile ->
                        newPlanFiles.any { it.absolutePath == mainFile.filePath }
                    } ?: false
                }
                // Add all files from the plan to the persistent service
                newestPlan?.allFiles?.forEach { file ->
                    project.service<PersistentFileService>().addFile(file)
                }
            }
        }
    }

    fun beforeCommandStarted() {
        val plansFolder = File(project.basePath, AiderPlanService.AIDER_PLANS_FOLDER)
        if (plansFolder.exists() && plansFolder.isDirectory) {
            initialPlanFiles = plansFolder.listFiles { file -> file.isFile && file.extension == "md" }
                ?.toSet() ?: emptySet()

            // Set active plan based on existing plan files
            val planService = project.service<AiderPlanService>()
            val plans = planService.getAiderPlans()
            
            // Find incomplete plans first
            val activePlan = plans.firstOrNull { !it.isPlanComplete() }
                ?: plans.lastOrNull() // If all plans are complete, take the last one

            // Add all files from the active plan to the persistent service
            activePlan?.allFiles?.forEach { file ->
                project.service<PersistentFileService>().addFile(file)
            }
        }
    }
}
