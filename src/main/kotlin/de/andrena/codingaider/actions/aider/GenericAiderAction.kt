package de.andrena.codingaider.actions.aider

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import de.andrena.codingaider.command.GenericCommandCollector
import de.andrena.codingaider.executors.CommandExecutorFactory
import de.andrena.codingaider.inputdialog.GenericAiderDialog
import de.andrena.codingaider.services.AiderDialogStateService
import de.andrena.codingaider.services.AiderIgnoreService
import de.andrena.codingaider.services.FileDataCollectionService
import de.andrena.codingaider.utils.AiderUtils
import de.andrena.codingaider.utils.NotificationUtils

/**
 * Generic Aider action that works with the CLI interface abstraction.
 * This replaces the original AiderAction to support multiple CLI tools.
 */
class GenericAiderAction : AnAction() {
    
    override fun actionPerformed(e: AnActionEvent) {
        executeGenericAiderAction(e, false)
    }
    
    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
        
        // Update presentation text based on selected CLI
        project?.let { 
            val genericSettings = de.andrena.codingaider.settings.GenericCliSettings.getInstance()
            e.presentation.text = "${genericSettings.selectedCli.replaceFirstChar { it.uppercase() }} Assistant"
            e.presentation.description = "Use ${genericSettings.selectedCli} AI assistant for coding tasks"
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    companion object {
        fun executeGenericAiderAction(e: AnActionEvent, directShellMode: Boolean) {
            val project: Project? = e.project
            val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            
            if (project != null && !files.isNullOrEmpty()) {
                val allFiles = project.service<FileDataCollectionService>().collectAllFiles(files)
                
                if (allFiles.isEmpty()) {
                    com.intellij.openapi.ui.Messages.showInfoMessage(
                        project,
                        "All selected files are ignored by the ignore configuration",
                        "No Files to Process"
                    )
                    return
                }
                
                if (directShellMode) {
                    executeShellMode(project, allFiles)
                } else {
                    showDialogMode(project, allFiles)
                }
            }
        }
        
        private fun executeShellMode(project: Project, files: List<de.andrena.codingaider.command.FileData>) {
            try {
                val commandData = GenericCommandCollector.collectForShellMode(files, project)
                val executor = CommandExecutorFactory.createExecutor(
                    de.andrena.codingaider.cli.CommandDataConverter.fromGenericCommandData(commandData),
                    project
                )
                executor.executeCommand()
            } catch (e: UnsupportedOperationException) {
                NotificationUtils.showError(
                    "Shell mode is not supported by the selected CLI tool",
                    project
                )
            }
        }
        
        private fun showDialogMode(project: Project, files: List<de.andrena.codingaider.command.FileData>) {
            val dialog = GenericAiderDialog(project, files)
            
            if (dialog.showAndGet()) {
                // Validate dialog input
                val validationErrors = dialog.validateInput()
                if (validationErrors.isNotEmpty()) {
                    val errorMessage = "Dialog validation errors:\n${validationErrors.joinToString("\n")}"
                    NotificationUtils.showError(errorMessage, project)
                    return
                }
                
                // Collect command data from dialog
                val commandData = GenericCommandCollector.collectFromDialog(dialog, project)
                
                // Save dialog state
                saveDialogState(dialog, project)
                
                // Execute the command
                try {
                    val executor = CommandExecutorFactory.createExecutor(
                        de.andrena.codingaider.cli.CommandDataConverter.fromGenericCommandData(commandData),
                        project
                    )
                    executor.executeCommand()
                } catch (e: Exception) {
                    de.andrena.codingaider.utils.AiderUtils.showBalloonNotification(
                        "Failed to execute command: ${e.message}",
                        project,
                        de.andrena.codingaider.utils.AiderUtils.NotificationType.ERROR
                    )
                }
            }
        }
        
        private fun saveDialogState(dialog: GenericAiderDialog, project: Project) {
            val stateService = AiderDialogStateService.getInstance(project)
            
            // Convert CliMode back to AiderMode for backward compatibility
            val aiderMode = when (dialog.getMode()) {
                de.andrena.codingaider.cli.CliMode.SHELL -> de.andrena.codingaider.inputdialog.AiderMode.SHELL
                de.andrena.codingaider.cli.CliMode.STRUCTURED -> de.andrena.codingaider.inputdialog.AiderMode.STRUCTURED
                de.andrena.codingaider.cli.CliMode.ARCHITECT -> de.andrena.codingaider.inputdialog.AiderMode.ARCHITECT
                else -> de.andrena.codingaider.inputdialog.AiderMode.NORMAL
            }
            
            stateService.saveState(
                dialog.getPrompt(),
                dialog.getYesFlag(),
                dialog.getModel(),
                dialog.getAdditionalArguments().map { "${it.key}=${it.value}" }.joinToString(" "),
                dialog.getFiles(),
                dialog.getMode() == de.andrena.codingaider.cli.CliMode.SHELL,
                dialog.getMode() == de.andrena.codingaider.cli.CliMode.STRUCTURED,
                aiderMode
            )
        }
        
        fun executeGenericAiderActionWithCommandData(
            project: Project, 
            genericCommandData: de.andrena.codingaider.cli.GenericCommandData
        ) {
            try {
                val legacyCommandData = de.andrena.codingaider.cli.CommandDataConverter.fromGenericCommandData(genericCommandData)
                val executor = CommandExecutorFactory.createExecutor(legacyCommandData, project)
                executor.executeCommand()
            } catch (e: Exception) {
                NotificationUtils.showError(
                    "Failed to execute command: ${e.message}",
                    project
                )
            }
        }
    }
}

/**
 * Generic Shell action for direct shell mode execution.
 */
class GenericAiderShellAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        GenericAiderAction.executeGenericAiderAction(e, true)
    }
    
    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val files: Array<VirtualFile>? = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        
        e.presentation.isEnabledAndVisible = project != null && !files.isNullOrEmpty()
        
        // Update presentation text based on selected CLI
        project?.let { 
            val genericSettings = de.andrena.codingaider.settings.GenericCliSettings.getInstance()
            e.presentation.text = "${genericSettings.selectedCli.replaceFirstChar { it.uppercase() }} Shell"
            e.presentation.description = "Use ${genericSettings.selectedCli} in shell mode"
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}