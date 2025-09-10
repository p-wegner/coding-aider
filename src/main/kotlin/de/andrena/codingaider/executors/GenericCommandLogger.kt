package de.andrena.codingaider.executors

import com.intellij.openapi.project.Project
import de.andrena.codingaider.cli.CliFactory
import de.andrena.codingaider.cli.CliInterface
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.settings.GenericCliSettings

/**
 * Generic command logger that works with different CLI tools.
 * This replaces the Aider-specific CommandLogger.
 */
class GenericCommandLogger(
    private val project: Project,
    private val genericSettings: GenericCliSettings,
    private val commandData: CommandData
) {
    
    /**
     * Gets the command string for logging purposes.
     * @param verbose Whether to include verbose information
     * @param dockerManager Optional Docker manager for containerized execution
     * @return The formatted command string
     */
    fun getCommandString(verbose: Boolean = false, dockerManager: DockerContainerManager? = null): String {
        val cliInterface = CliFactory.getCurrentCli()
        val executable = cliInterface?.getExecutableName() ?: "unknown"
        
        return buildString {
            append("=== ${cliInterface?.getExecutableName()?.uppercase()} COMMAND ===\n")
            append("Executable: $executable\n")
            
            if (verbose) {
                append("Project: ${commandData.projectPath}\n")
                append("Files: ${commandData.files.size}\n")
                
                if (commandData.files.isNotEmpty()) {
                    append("File List:\n")
                    commandData.files.forEach { fileData ->
                        append("  - ${fileData.filePath} (${if (fileData.isReadOnly) "read-only" else "writable"})\n")
                    }
                }
            }
            
            // Build command args
            val commandArgs = cliInterface?.buildCommand(commandData) ?: emptyList()
            append("Command: ${commandArgs.joinToString(" ")}\n")
            
            // Add Docker information if applicable
            if (dockerManager != null) {
                append("Docker: Enabled\n")
            }
            
            // Add CLI-specific information
            cliInterface?.let { cli ->
                append("CLI Features: Basic execution\n")
            }
            
            append("=== END COMMAND ===\n")
        }
    }
    
    /**
     * Gets the execution summary.
     * @return The execution summary string
     */
    fun getExecutionSummary(): String {
        val cliInterface = CliFactory.getCurrentCli()
        
        return buildString {
            append("Execution Summary:\n")
            append("  CLI Tool: ${cliInterface?.getExecutableName() ?: "unknown"}\n")
            append("  Model: ${commandData.llm}\n")
            append("  Files: ${commandData.files.size}\n")
            append("  Mode: ${commandData.aiderMode}\n")
            append("  Project: ${commandData.projectPath}\n")
            
            if (commandData.sidecarMode) {
                append("  Sidecar Mode: Enabled\n")
            }
            
            if (genericSettings.commonExecutionOptions.useDocker) {
                append("  Docker: Enabled\n")
            }
        }
    }
    
    /**
     * Logs the command execution start.
     */
    fun logExecutionStart() {
        val cliInterface = CliFactory.getCurrentCli()
        println("Starting ${cliInterface?.getExecutableName()} execution...")
        println(getExecutionSummary())
    }
    
    /**
     * Logs the command execution completion.
     * @param exitCode The exit code of the process
     * @param output The output of the process
     */
    fun logExecutionCompletion(exitCode: Int, output: String) {
        val cliInterface = CliFactory.getCurrentCli()
        println("${cliInterface?.getExecutableName()} execution completed with exit code: $exitCode")
        if (false) { // verboseLogging disabled for now
            println("Output length: ${output.length} characters")
        }
    }
    
    /**
     * Logs an error during command execution.
     * @param error The error message
     * @param throwable Optional throwable for detailed error information
     */
    fun logError(error: String, throwable: Throwable? = null) {
        val cliInterface = CliFactory.getCurrentCli()
        System.err.println("Error in ${cliInterface?.getExecutableName()} execution: $error")
        throwable?.printStackTrace()
    }
    
    /**
     * Gets a formatted timestamp for logging.
     * @return The formatted timestamp
     */
    private fun getTimestamp(): String {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
    
    companion object {
        /**
         * Creates a command logger for the specified CLI tool.
         * @param project The project
         * @param commandData The command data
         * @return The command logger
         */
        fun create(project: Project, commandData: CommandData): GenericCommandLogger {
            val genericSettings = GenericCliSettings.getInstance()
            return GenericCommandLogger(project, genericSettings, commandData)
        }
    }
}