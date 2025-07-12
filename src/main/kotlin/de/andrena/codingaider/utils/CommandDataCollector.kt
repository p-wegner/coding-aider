package de.andrena.codingaider.utils

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.inputdialog.AiderInputDialog
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance

/**
 * Utility class for collecting and creating CommandData objects from various sources.
 * This centralizes the logic for building CommandData to avoid duplication.
 */
object CommandDataCollector {
    
    /**
     * Collects command data from an AiderInputDialog.
     */
    fun collectFromDialog(dialog: AiderInputDialog, project: Project): CommandData {
        val settings = getInstance()
        return CommandData(
            message = dialog.getInputText(),
            useYesFlag = dialog.isYesFlagChecked(),
            llm = dialog.getLlm().name,
            additionalArgs = dialog.getAdditionalArgs(),
            files = dialog.getAllFiles(),
            lintCmd = settings.lintCmd,
            deactivateRepoMap = settings.deactivateRepoMap,
            editFormat = settings.editFormat,
            projectPath = project.basePath ?: "",
            aiderMode = dialog.selectedMode,
            sidecarMode = settings.useSidecarMode
        )
    }
    
    /**
     * Collects command data from individual parameters.
     */
    fun collectFromParameters(
        files: List<FileData>, 
        message: String, 
        project: Project, 
        mode: AiderMode
    ): CommandData {
        val settings = getInstance()
        return CommandData(
            message = message,
            useYesFlag = settings.useYesFlag,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs,
            files = files,
            lintCmd = settings.lintCmd,
            deactivateRepoMap = settings.deactivateRepoMap,
            editFormat = settings.editFormat,
            projectPath = project.basePath ?: "",
            aiderMode = mode,
            sidecarMode = settings.useSidecarMode
        )
    }
    
    /**
     * Collects default shell command data for shell mode execution.
     */
    fun collectForShellMode(files: List<FileData>, project: Project): CommandData {
        val settings = getInstance()
        return CommandData(
            message = "",
            useYesFlag = settings.useYesFlag,
            llm = settings.llm,
            additionalArgs = settings.additionalArgs,
            files = files,
            lintCmd = settings.lintCmd,
            deactivateRepoMap = settings.deactivateRepoMap,
            editFormat = settings.editFormat,
            projectPath = project.basePath ?: "",
            aiderMode = AiderMode.SHELL,
            sidecarMode = settings.useSidecarMode
        )
    }
}
