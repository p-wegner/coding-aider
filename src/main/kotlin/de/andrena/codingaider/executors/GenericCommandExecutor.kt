package de.andrena.codingaider.executors

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.cli.CliFactory
import de.andrena.codingaider.cli.CliInterface
import de.andrena.codingaider.cli.CommandDataConverter
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.executors.api.AiderProcessInteractor
import de.andrena.codingaider.executors.api.CommandSubject
import de.andrena.codingaider.executors.api.DefaultAiderProcessInteractor
import de.andrena.codingaider.services.FileExtractorService
import de.andrena.codingaider.services.PluginBasedEditsService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.sidecar.AiderProcessManager
import de.andrena.codingaider.services.sidecar.SidecarProcessInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import de.andrena.codingaider.settings.AiderProjectSettings
import de.andrena.codingaider.settings.GenericCliSettings
import de.andrena.codingaider.utils.ApiKeyChecker
import de.andrena.codingaider.utils.DefaultApiKeyChecker
import java.io.File

/**
 * Generic command executor that uses CLI interfaces instead of Aider-specific strategies.
 * This provides a unified execution interface for different CLI tools.
 */
class GenericCommandExecutor(
    private val commandData: CommandData,
    private val project: Project,
    private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
) : CommandSubject by GenericCommandSubject(), CommandExecutorInterface {
    
    private val logger = Logger.getInstance(GenericCommandExecutor::class.java)
    private val genericSettings = GenericCliSettings.getInstance()
    private val commandLogger = GenericCommandLogger(project, genericSettings, commandData)
    private var process: Process? = null
    private var isAborted = false
    private var startTime = 0L
    
    // CLI interface for the selected tool
    val cliInterface: CliInterface by lazy {
        CliFactory.getCurrentCli() ?: throw IllegalStateException("No CLI tool selected")
    }
    
    // Docker manager for containerized execution
    private val useDocker: Boolean by lazy {
        commandData.options.useDockerAider ?: genericSettings.commonExecutionOptions.useDocker
    }
    private val dockerManager = DockerContainerManager()
    
    // Process interactor for Aider-specific operations
    private val processInteractor: AiderProcessInteractor by lazy { 
        DefaultAiderProcessInteractor(project) 
    }
    
    // Services
    private val aiderPlanService = project.service<AiderPlanService>()
    
    /**
     * Executes the command using the selected CLI interface.
     */
    override fun executeCommand(): String {
        aiderPlanService.createPlanFolderIfNeeded(commandData)
        val updatedCommandData = extractFilesIfNeeded()
        
        // Track plan creation if this is a structured mode command
        if (commandData.structuredMode && commandData.planId == null) {
            aiderPlanService.trackPlanCreation(commandData)
        }
        
        if (commandData.sidecarMode) {
            return startSideCarAndExecuteCommand(updatedCommandData)
        }
        
        return executeCommandInNewProcess(updatedCommandData)
    }
    
    /**
     * Executes the command in a new process using the CLI interface.
     */
    private fun executeCommandInNewProcess(updatedCommandData: CommandData): String {
        startTime = System.currentTimeMillis()
        
        // Validate command data with CLI interface
        val validationErrors = cliInterface.validateCommandData(updatedCommandData)
        if (validationErrors.isNotEmpty()) {
            val errorMsg = "Command validation failed: ${validationErrors.joinToString(", ")}"
            logger.error(errorMsg)
            notifyObservers { it.onCommandError(errorMsg) }
            return errorMsg
        }
        
        // Build command using CLI interface
        val commandArgs = cliInterface.buildCommand(updatedCommandData)
        logger.info("Executing ${cliInterface.getExecutableName()} command: ${commandArgs.joinToString(" ")}")
        
        notifyObservers {
            it.onCommandStart(
                "\n${
                    commandLogger.getCommandString(
                        false,
                        if (useDocker) dockerManager else null
                    )
                }"
            )
        }
        
        // Create process builder
        val mutableCommandArgs = commandArgs.toMutableList()
        val processBuilder = ProcessBuilder(mutableCommandArgs)
            .apply {
                setWorkingDirAndSubtreeIfNeeded(mutableCommandArgs)
                environment().putIfAbsent("PYTHONIOENCODING", "utf-8")
                redirectErrorStream(true)
            }
        
        // Prepare environment using CLI interface
        cliInterface.prepareEnvironment(processBuilder, updatedCommandData)
        
        // Start the process
        process = processBuilder.start()
        
        val output = StringBuilder()
        try {
            pollProcessAndReadOutput(process!!, output)
            return handleProcessCompletion(process!!, output)
        } finally {
            cliInterface.cleanupAfterExecution()
        }
    }
    
    /**
     * Sets the working directory and adds subtree-only flag if needed.
     */
    private fun ProcessBuilder.setWorkingDirAndSubtreeIfNeeded(commandArgs: MutableList<String>) {
        val workingDir = project.service<AiderProjectSettings>().workingDirectory
        if (workingDir != null && workingDir.isNotEmpty()) {
            val normalizedWorkingDir = File(workingDir).canonicalPath
            val normalizedProjectPath = File(commandData.projectPath.ifEmpty { System.getProperty("user.home") }).canonicalPath
            
            if (normalizedWorkingDir.startsWith(normalizedProjectPath)) {
                val workingDirFile = File(normalizedWorkingDir)
                if (workingDirFile.exists() && workingDirFile.isDirectory) {
                    directory(workingDirFile)
                    // Add --subtree-only flag when working directory is set (Aider-specific)
                    if (!commandArgs.contains("--subtree-only") && cliInterface.getExecutableName() == "aider") {
                        commandArgs.add("--subtree-only")
                    }
                } else {
                    logger.warn("Working directory $normalizedWorkingDir does not exist or is not a directory")
                }
            }
        }
    }
    
    /**
     * Starts sidecar mode and executes the command.
     */
    private fun startSideCarAndExecuteCommand(updatedCommandData: CommandData): String {
        val aiderProcessManager = project.getService<AiderProcessManager>(AiderProcessManager::class.java)
        val sidecarProcessInitializer = SidecarProcessInitializer(project, CoroutineScope(Dispatchers.IO))
        
        try {
            if (!aiderProcessManager.isReadyForCommand()) {
                sidecarProcessInitializer.initializeSidecarProcess()
            }
            
            val sidecarCommand = buildSidecarCommandString(updatedCommandData)
            return executeSidecarCommand(sidecarCommand)
        } catch (e: Exception) {
            logger.error("Error in sidecar mode execution", e)
            return "Error in sidecar mode execution: ${e.message}"
        }
    }
    
    /**
     * Builds the sidecar command string.
     */
    private fun buildSidecarCommandString(commandData: CommandData): String {
        // For now, keep the original sidecar logic
        // This could be abstracted further in the future
        return commandData.message
    }
    
    /**
     * Executes a command in sidecar mode.
     */
    private fun executeSidecarCommand(command: String): String {
        // Implementation for sidecar command execution
        // This would be abstracted to work with different CLI tools in the future
        val aiderProcessManager = project.getService<AiderProcessManager>(AiderProcessManager::class.java)
        
        if (aiderProcessManager.isReadyForCommand()) {
            return processInteractor.sendCommandSync(command)
        } else {
            return "Sidecar process not available"
        }
    }
    
    /**
     * Polls the process and reads its output.
     */
    private fun pollProcessAndReadOutput(process: Process, output: StringBuilder) {
        process.inputStream.bufferedReader().use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (isAborted) {
                    process.destroy()
                    break
                }
                line?.let {
                    output.append(it).append("\n")
                    notifyObservers { observer -> observer.onCommandProgress(it, System.currentTimeMillis() - startTime) }
                }
            }
        }
    }
    
    /**
     * Handles process completion and performs post-processing.
     */
    private fun handleProcessCompletion(process: Process, output: StringBuilder): String {
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            logger.warn("Process exited with code $exitCode")
            notifyObservers { it.onCommandError("Process exited with code $exitCode") }
        } else {
            notifyObservers { it.onCommandComplete(output.toString(), exitCode) }
        }
        
        // Post-process the output if needed
        val finalOutput = postProcessOutput(output.toString())
        
        // Handle plugin-based edits if enabled (Aider-specific)
        if (cliInterface.getExecutableName() == "aider" && shouldProcessPluginBasedEdits()) {
            processPluginBasedEdits(finalOutput)
        }
        
        return finalOutput
    }
    
    /**
     * Post-processes the command output.
     */
    private fun postProcessOutput(output: String): String {
        // Add any post-processing logic here
        return output
    }
    
    /**
     * Checks if plugin-based edits should be processed.
     */
    private fun shouldProcessPluginBasedEdits(): Boolean {
        val aiderSettings = de.andrena.codingaider.settings.AiderSpecificSettings.getInstance()
        return aiderSettings.state.pluginBasedEdits
    }
    
    /**
     * Processes plugin-based edits (Aider-specific).
     */
    private fun processPluginBasedEdits(output: String) {
        try {
            val pluginBasedEditsService = project.service<PluginBasedEditsService>()
            pluginBasedEditsService.processLlmResponse(output)
        } catch (e: Exception) {
            logger.error("Error processing plugin-based edits", e)
        }
    }
    
    /**
     * Extracts files if needed for plan context.
     */
    private fun extractFilesIfNeeded(): CommandData {
        return if (commandData.structuredMode && commandData.planId != null) {
            val fileExtractorService = project.service<FileExtractorService>()
            val extractedFiles = fileExtractorService.extractFilesIfNeeded(commandData.files)
            commandData.copy(files = extractedFiles)
        } else {
            commandData
        }
    }
    
    /**
     * Aborts the current command execution.
     */
    override fun abort() {
        isAborted = true
        process?.destroy()
    }
    
        
    /**
     * Gets information about the current CLI tool.
     */
    override fun getCliInfo(): String {
        return "Using CLI: ${cliInterface.getExecutableName()} with ${commandData.files.size} files"
    }
    
    }