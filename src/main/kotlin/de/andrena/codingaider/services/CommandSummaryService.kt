package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.plans.AiderPlanPromptService

@Service(Service.Level.PROJECT)
class CommandSummaryService(private val project: Project) {

    fun generateSummary(commandData: CommandData): String {
        return buildString {
            val message = commandData.message
            when (commandData.aiderMode) {
                AiderMode.ARCHITECT -> append("[ARCHITECT]")
                AiderMode.STRUCTURED -> append("[STRUCTURED]")
                AiderMode.SHELL -> append("[SHELL]")
                AiderMode.NORMAL -> append("[NORMAL]")
            }
            if (commandData.structuredMode) {
                val planService = project.service<AiderPlanPromptService>()
                planService.filterPlanMainFiles(commandData.files).firstOrNull()?.let { planFile ->
                    val planName = planFile.filePath
                        .substringAfterLast("/")
                        .substringAfterLast("\\")
                        .substringBeforeLast(".")
                    append(" - ")
                    append(planName)
                } ?: append(message.abbreviatedMessage(20))
            } else
                append(message.abbreviatedMessage(20))
        }
    }

    private fun String.abbreviatedMessage(maxLength: Int): String =
        if (length > maxLength) {
            "${substring(0, maxLength)}..."
        } else {
            this
        }
}
