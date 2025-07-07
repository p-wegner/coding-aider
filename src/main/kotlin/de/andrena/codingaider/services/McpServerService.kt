package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.modelcontextprotocol.kotlin.sdk.server.McpServer
import io.modelcontextprotocol.kotlin.sdk.server.McpServerBuilder
import io.modelcontextprotocol.kotlin.sdk.shared.Tool
import io.modelcontextprotocol.kotlin.sdk.shared.ToolCall
import io.modelcontextprotocol.kotlin.sdk.shared.ToolResult
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class McpServerService(private val project: Project) {
    private val logger = Logger.getInstance(McpServerService::class.java)
    private var mcpServer: McpServer? = null
    private val isRunning = AtomicBoolean(false)
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startServer() {
        if (isRunning.get()) {
            logger.info("MCP server is already running")
            return
        }

        try {
            val server = McpServerBuilder()
                .withName("coding-aider-plugin")
                .withVersion("1.0.0")
                .addTool(createRepoMapTool())
                .build()

            mcpServer = server
            
            serverJob = scope.launch {
                try {
                    isRunning.set(true)
                    logger.info("Starting MCP server for project: ${project.name}")
                    server.start()
                } catch (e: Exception) {
                    logger.error("Failed to start MCP server", e)
                    isRunning.set(false)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize MCP server", e)
        }
    }

    fun stopServer() {
        if (!isRunning.get()) {
            return
        }

        try {
            serverJob?.cancel()
            mcpServer?.stop()
            isRunning.set(false)
            logger.info("MCP server stopped")
        } catch (e: Exception) {
            logger.error("Error stopping MCP server", e)
        }
    }

    private fun createRepoMapTool(): Tool {
        return Tool(
            name = "get_repo_map",
            description = "Retrieves the repository map for the current project using Aider's repo mapping functionality",
            inputSchema = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "include_patterns" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "string"),
                        "description" to "Optional file patterns to include in the repo map"
                    ),
                    "exclude_patterns" to mapOf(
                        "type" to "array", 
                        "items" to mapOf("type" to "string"),
                        "description" to "Optional file patterns to exclude from the repo map"
                    )
                )
            )
        ) { toolCall ->
            handleRepoMapTool(toolCall)
        }
    }

    private suspend fun handleRepoMapTool(toolCall: ToolCall): ToolResult {
        return try {
            logger.info("Handling repo map tool call")
            
            val repoMapService = project.getService(RepoMapService::class.java)
            val repoMap = repoMapService.generateRepoMap()
            
            ToolResult.success(
                content = listOf(
                    mapOf(
                        "type" to "text",
                        "text" to repoMap
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("Error generating repo map", e)
            ToolResult.error("Failed to generate repository map: ${e.message}")
        }
    }

    fun isServerRunning(): Boolean = isRunning.get()

    fun dispose() {
        stopServer()
        scope.cancel()
    }

    companion object {
        fun getInstance(project: Project): McpServerService =
            project.getService(McpServerService::class.java)
    }
}
