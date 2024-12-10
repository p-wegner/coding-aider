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
        }
        // TODO: Set Active Plan when plan file is created in plan folder

    }

    fun beforeCommandStarted() {
        // TODO: Set Active Plan depending on plan file in plan folder

        val plansFolder = File(project.basePath, AiderPlanService.AIDER_PLANS_FOLDER)
        if (plansFolder.exists() && plansFolder.isDirectory) {
            initialPlanFiles = plansFolder.listFiles { file -> file.isFile && file.extension == "md" }
                ?.toSet() ?: emptySet()
        }
    }

}