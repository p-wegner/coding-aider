package de.andrena.codingaider.command

import de.andrena.codingaider.cli.CliMode
import com.intellij.openapi.project.Project

/**
 * Generic dialog interface for CLI tools.
 * This abstracts the dialog functionality to work with different CLI tools.
 */
interface GenericDialog {
    /**
     * Gets the prompt/message from the dialog.
     * @return The prompt text
     */
    fun getPrompt(): String
    
    /**
     * Gets the selected model from the dialog.
     * @return The model name
     */
    fun getModel(): String
    
    /**
     * Gets whether the yes flag is enabled.
     * @return true if yes flag is enabled, false otherwise
     */
    fun getYesFlag(): Boolean
    
    /**
     * Gets the additional arguments from the dialog.
     * @return Map of additional arguments
     */
    fun getAdditionalArguments(): Map<String, String>
    
    /**
     * Gets the list of files selected in the dialog.
     * @return List of file data
     */
    fun getFiles(): List<FileData>
    
    /**
     * Gets the selected execution mode.
     * @return The CLI mode
     */
    fun getMode(): CliMode
    
    /**
     * Gets the project context.
     * @return The project
     */
    fun getProject(): Project
    
    /**
     * Shows the dialog and waits for user input.
     * @return true if the user clicked OK, false if cancelled
     */
    fun showAndGet(): Boolean
    
    /**
     * Gets the CLI-specific options.
     * @return CLI-specific options map
     */
    fun getCliSpecificOptions(): Map<String, Any>
    
    /**
     * Validates the dialog input.
     * @return List of validation errors, empty if valid
     */
    fun validateInput(): List<String>
    
    /**
     * Gets the dialog title.
     * @return The dialog title
     */
    fun getDialogTitle(): String
    
    /**
     * Gets the CLI tool name this dialog is for.
     * @return The CLI tool name
     */
    fun getCliToolName(): String
}