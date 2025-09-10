package de.andrena.codingaider.cli

import de.andrena.codingaider.command.FileData

/**
 * Generic command data structure that can be used by different CLI tools.
 * This abstracts away the Aider-specific parameters from the original CommandData.
 */
data class GenericCommandData(
    /**
     * The main prompt or instruction for the AI
     */
    val prompt: String,
    
    /**
     * The language model to use
     */
    val model: String,
    
    /**
     * List of files to be included in the command
     */
    val files: List<FileData>,
    
    /**
     * Generic command options
     */
    val options: GenericCommandOptions,
    
    /**
     * The execution mode for the command
     */
    val cliMode: CliMode,
    
    /**
     * The project path
     */
    val projectPath: String,
    
    /**
     * Additional CLI-specific arguments
     */
    val additionalArgs: Map<String, String> = emptyMap(),
    
    /**
     * Working directory for the command
     */
    val workingDir: String = "",
    
    /**
     * Plan ID for structured mode
     */
    val planId: String? = null,
    
    /**
     * Start time of the command
     */
    val startTime: Long = System.currentTimeMillis()
) {
    /**
     * Checks if any file is read-only
     */
    fun hasReadOnlyFiles(): Boolean {
        return files.any { it.isReadOnly }
    }
    
    /**
     * Gets all file paths
     */
    fun getFilePaths(): List<String> {
        return files.map { it.filePath }
    }
    
    /**
     * Gets all read-only file paths
     */
    fun getReadOnlyFilePaths(): List<String> {
        return files.filter { it.isReadOnly }.map { it.filePath }
    }
    
    /**
     * Gets all writable file paths
     */
    fun getWritableFilePaths(): List<String> {
        return files.filter { !it.isReadOnly }.map { it.filePath }
    }
    
    /**
     * Validates the command data
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (prompt.isBlank()) {
            errors.add("Prompt cannot be blank")
        }
        
        if (model.isBlank()) {
            errors.add("Model cannot be blank")
        }
        
        if (projectPath.isBlank()) {
            errors.add("Project path cannot be blank")
        }
        
        // Validate file paths
        files.forEach { fileData ->
            if (fileData.filePath.isBlank()) {
                errors.add("File path cannot be blank")
            }
        }
        
        return errors
    }
}