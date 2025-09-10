package de.andrena.codingaider.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import de.andrena.codingaider.cli.GenericCommandData
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.command.GenericCommandCollector
import de.andrena.codingaider.command.GenericDialog
import de.andrena.codingaider.executors.CommandExecutorFactory
import de.andrena.codingaider.utils.AiderUtils

/**
 * Generic action for CLI tools.
 * This abstracts the action functionality to work with different CLI tools.
 */
abstract class GenericCliAction : AnAction() {
    
    /**
     * Gets the CLI tool name for this action.
     * @return The CLI tool name
     */
    abstract fun getCliToolName(): String
    
    /**
     * Creates the dialog for this CLI tool.
     * @param project The project context
     * @param event The action event
     * @return The dialog instance
     */
    abstract fun createDialog(project: Project, event: AnActionEvent): GenericDialog
    
    /**
     * Executes the CLI action with the collected command data.
     * @param commandData The command data to execute
     * @param project The project context
     */
    protected open fun executeCliAction(commandData: CommandData, project: Project) {
        val executor = CommandExecutorFactory.createExecutor(commandData, project)
        executor.executeCommand()
    }
    
    /**
     * Gets the selected files for the action.
     * @param event The action event
     * @return List of selected files
     */
    protected open fun getSelectedFiles(event: AnActionEvent): List<FileData> {
        return AiderUtils.getSelectedFiles(event).map {
            FileData(it.filePath, false)
        }
    }
    
    /**
     * Validates that the CLI tool is available and properly configured.
     * @param event The action event
     * @return true if the CLI tool is available, false otherwise
     */
    protected open fun validateCliTool(event: AnActionEvent): Boolean {
        val cliName = getCliToolName()
        val validationErrors = CommandExecutorFactory.validateCliConfiguration(cliName)
        
        if (validationErrors.isNotEmpty()) {
            val errorMessage = "CLI tool '$cliName' configuration issues:\n${validationErrors.joinToString("\n")}"
            AiderUtils.showBalloonNotification(errorMessage, event.project)
            return false
        }
        
        return true
    }
    
    /**
     * Checks if the CLI tool supports specific features.
     * @param event The action event
     * @param requiredFeatures Set of required features
     * @return true if all features are supported, false otherwise
     */
    protected open fun validateCliFeatures(event: AnActionEvent, requiredFeatures: Set<de.andrena.codingaider.cli.CliFeature>): Boolean {
        val cliName = getCliToolName()
        val supportsFeatures = CommandExecutorFactory.supportsFeatures(cliName, requiredFeatures)
        
        if (!supportsFeatures) {
            val missingFeatures = requiredFeatures.filter { !CommandExecutorFactory.supportsFeatures(cliName, setOf(it)) }
            val errorMessage = "CLI tool '$cliName' does not support required features:\n${missingFeatures.joinToString("\n") { it.name.replace("_", " ").lowercase() }}"
            AiderUtils.showBalloonNotification(errorMessage, event.project)
            return false
        }
        
        return true
    }
    
    /**
     * Shows a dialog and collects command data.
     * @param project The project context
     * @param event The action event
     * @param selectedFiles List of selected files
     * @return The collected command data, or null if cancelled
     */
    protected open fun collectCommandDataFromDialog(
        project: Project, 
        event: AnActionEvent, 
        selectedFiles: List<FileData>
    ): GenericCommandData? {
        val dialog = createDialog(project, event)
        
        if (!dialog.showAndGet()) {
            return null // User cancelled
        }
        
        // Validate dialog input
        val validationErrors = dialog.validateInput()
        if (validationErrors.isNotEmpty()) {
            val errorMessage = "Dialog validation errors:\n${validationErrors.joinToString("\n")}"
            AiderUtils.showBalloonNotification(errorMessage, project)
            return null
        }
        
        // Collect command data
        return GenericCommandCollector.collectFromDialog(dialog, project)
    }
    
    /**
     * Shows a notification to the user.
     * @param message The message to show
     * @param project The project context
     * @param type The notification type
     */
    protected open fun showNotification(message: String, project: Project?, type: AiderUtils.NotificationType = AiderUtils.NotificationType.INFO) {
        AiderUtils.showBalloonNotification(message, project, type)
    }
    
    /**
     * Gets the action text for the CLI tool.
     * @return The action text
     */
    protected open fun getActionText(): String {
        return "${getCliToolName().replaceFirstChar { it.uppercase() }} Assistant"
    }
    
    /**
     * Gets the action description for the CLI tool.
     * @return The action description
     */
    protected open fun getActionDescription(): String {
        return "Use ${getCliToolName()} AI assistant for coding tasks"
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        
        // Check if CLI tool is available
        val cliAvailable = validateCliTool(e)
        e.presentation.isEnabled = cliAvailable
        
        // Update presentation text
        e.presentation.text = getActionText()
        e.presentation.description = getActionDescription()
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // Validate CLI tool configuration
        if (!validateCliTool(e)) {
            return
        }
        
        // Get selected files
        val selectedFiles = getSelectedFiles(e)
        
        // Show dialog and collect command data
        val commandData = collectCommandDataFromDialog(project, e, selectedFiles) ?: return
        
        // Convert to legacy format for now (backward compatibility)
        val legacyCommandData = GenericCommandCollector.toLegacyCommandData(commandData)
        
        // Execute the action
        executeCliAction(legacyCommandData, project)
    }
}