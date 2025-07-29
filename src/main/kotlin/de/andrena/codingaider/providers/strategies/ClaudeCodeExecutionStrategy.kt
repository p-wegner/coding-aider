package de.andrena.codingaider.providers.strategies

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.providers.AIExecutionStrategy
import de.andrena.codingaider.providers.AIProvider

/**
 * Base execution strategy for Claude Code provider
 */
abstract class ClaudeCodeExecutionStrategy(override val project: Project) : AIExecutionStrategy {
    override val provider: AIProvider = AIProvider.CLAUDE_CODE
    
    abstract override fun buildCommand(commandData: CommandData): MutableList<String>
    abstract override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData)
    abstract override fun cleanupAfterExecution()
    
    /**
     * Default implementation that checks for Claude Code availability
     */
    override fun isProviderAvailable(): Boolean {
        return try {
            val process = ProcessBuilder(getExecutableName(), "update").start()
            process.waitFor()
            // Claude Code's update command returns 0 when available
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the display name for this Claude Code execution strategy
     */
    override fun getDisplayName(): String = "Claude Code (${getStrategyType()})"
    
    /**
     * Abstract method for concrete strategies to define their type
     */
    protected abstract fun getStrategyType(): String
    
    /**
     * Gets the executable name/path for Claude Code
     * Can be overridden by concrete strategies
     */
    protected open fun getExecutableName(): String = "claude"
    
    /**
     * Builds common arguments that all Claude Code strategies can use
     * Based on actual Claude Code CLI interface
     */
    protected fun buildCommonClaudeArgs(commandData: CommandData): MutableList<String> {
        return buildList {
            // Claude Code uses print mode (-p) for non-interactive usage
            add("-p")
            
            // Add the message as the query parameter (Claude Code takes the prompt as an argument)
            if (commandData.message.isNotEmpty()) {
                add(commandData.message)
            }
            
            // Add additional directories if files are outside current directory
            val additionalDirs = mutableSetOf<String>()
            commandData.files.forEach { fileData ->
                val fileDir = java.io.File(fileData.filePath).parentFile?.absolutePath
                if (fileDir != null && fileDir != commandData.projectPath) {
                    additionalDirs.add(fileDir)
                }
            }
            
            // Add --add-dir flags for additional directories
            if (additionalDirs.isNotEmpty()) {
                add("--add-dir")
                addAll(additionalDirs)
            }
            
            // Handle verbose mode if enabled in additional args
            if (commandData.additionalArgs.contains("--verbose") || commandData.additionalArgs.contains("-v")) {
                add("--verbose")
            }
            
            // Handle permission modes
            if (commandData.useYesFlag) {
                add("--dangerously-skip-permissions")
            }
            
            // Add any additional custom arguments that are valid for Claude Code
            if (commandData.additionalArgs.isNotEmpty()) {
                val validArgs = commandData.additionalArgs.split(" ").filter { arg ->
                    // Only include arguments that are valid for Claude Code
                    arg.startsWith("--model") || 
                    arg.startsWith("--max-turns") ||
                    arg.startsWith("--output-format") ||
                    arg.startsWith("--permission-mode") ||
                    arg == "--verbose" || arg == "-v"
                }
                addAll(validArgs)
            }
            
        }.toMutableList()
    }
}