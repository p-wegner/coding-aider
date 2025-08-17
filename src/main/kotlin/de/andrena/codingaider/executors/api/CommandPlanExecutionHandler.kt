package de.andrena.codingaider.executors.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.plans.ActivePlanService
import de.andrena.codingaider.services.plans.AiderPlanService
import java.io.File


class CommandPlanExecutionHandler(private val project: Project, private val commandData: CommandData) {
    private var initialPlanFiles: Set<File> = emptySet()

    fun commandCompleted() {
        if (!commandData.structuredMode) return
        val planService = project.service<AiderPlanService>()
        val plansFolder = File(project.basePath, planService.getAiderPlansFolder())
        if (plansFolder.exists() && plansFolder.isDirectory) {
            val currentPlanFiles = plansFolder.listFiles { file -> file.isFile && file.extension == "md" }
                ?.toSet() ?: emptySet()

            val newPlanFiles = currentPlanFiles.subtract(initialPlanFiles)
            if (newPlanFiles.isNotEmpty()) {
                val plans = project.service<AiderPlanService>().getAiderPlans(newPlanFiles.toList())
                plans.firstOrNull()?.let {
                    project.service<ActivePlanService>().setActivePlan(it)
                    project.service<PersistentFileService>().addAllFiles(it.allFiles)
                }
            }
        }
    }

    fun beforeCommandStarted() {
        if (!commandData.structuredMode) return
        val planService = project.service<AiderPlanService>()
        val plansFolder = File(project.basePath, planService.getAiderPlansFolder())
        if (plansFolder.exists() && plansFolder.isDirectory) {
            initialPlanFiles = plansFolder.listFiles { file -> file.isFile && file.extension == "md" }
                ?.toSet() ?: emptySet()

            // Set active plan based on files from commandData
            val files: List<File> = commandData.files.map { File(it.filePath) }
            project.service<AiderPlanService>().getAiderPlans(files).firstOrNull()?.let {
                project.service<ActivePlanService>().setActivePlan(it)
                project.service<PersistentFileService>().addAllFiles(it.allFiles)
            }
        }
    }
}
