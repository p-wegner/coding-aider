package de.andrena.codingaider.providers.strategies

import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.utils.ApiKeyChecker

/**
 * Execution strategy for Claude Code using native installation
 */
class ClaudeCodeNativeExecutionStrategy(
    project: Project,
    private val apiKeyChecker: ApiKeyChecker,
    private val settings: AiderSettings
) : ClaudeCodeExecutionStrategy(project) {
    
    override fun getStrategyType(): String = "Native"
    
    override fun buildCommand(commandData: CommandData): MutableList<String> {
        return (listOf(getClaudeCodeExecutablePath()) + buildCommonClaudeArgs(commandData)).toMutableList()
    }
    
    override fun prepareEnvironment(processBuilder: ProcessBuilder, commandData: CommandData) {
        val environment = processBuilder.environment()
        
        // Set up API keys - Claude Code uses ANTHROPIC_API_KEY
        apiKeyChecker.getApiKeyValue("ANTHROPIC_API_KEY")?.let { key ->
            environment["ANTHROPIC_API_KEY"] = key
        }
        
        // Set working directory to project root since Claude Code works with directory context
        if (commandData.projectPath.isNotEmpty()) {
            processBuilder.directory(java.io.File(commandData.projectPath))
        }
        
        // Add other environment variables as needed
        environment.putIfAbsent("PYTHONIOENCODING", "utf-8")
    }
    
    override fun cleanupAfterExecution() {
        // No specific cleanup needed for native Claude Code execution
    }
    
    /**
     * Gets the path to the Claude Code executable
     * Uses the configured path from settings, defaulting to "claude"
     */
    private fun getClaudeCodeExecutablePath(): String {
        return settings.claudeCodeExecutablePath.takeIf { it.isNotEmpty() } ?: getExecutableName()
    }
}