package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.sse.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class McpServerService(private val project: Project) {
    private val logger = Logger.getInstance(McpServerService::class.java)
    private var ktorServer: ApplicationEngine? = null
    private var mcpServer: Server? = null
    private val isRunning = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        const val DEFAULT_PORT = 8080
        const val MCP_ENDPOINT = "/mcp"
        const val SSE_ENDPOINT = "/sse"
    }

    fun startServer(port: Int = DEFAULT_PORT) {
        if (isRunning.get()) {
            logger.warn("MCP Server is already running")
            return
        }

        try {
            serviceScope.launch {
                mcpServer = createMcpServer()
                ktorServer = embeddedServer(Netty, port = port) {
                    install(SSE)
                    mcp {
                        mcpServer!!
                    }
                }.start(wait = false)
                
                isRunning.set(true)
                logger.info("MCP Server started on port $port")
            }
        } catch (e: Exception) {
            logger.error("Failed to start MCP Server", e)
            isRunning.set(false)
        }
    }

    fun stopServer() {
        if (!isRunning.get()) {
            return
        }

        try {
            serviceScope.launch {
                ktorServer?.stop(1000, 2000)
                ktorServer = null
                mcpServer = null
                isRunning.set(false)
                logger.info("MCP Server stopped")
            }
        } catch (e: Exception) {
            logger.error("Error stopping MCP Server", e)
        }
    }

    fun isServerRunning(): Boolean = isRunning.get()

    fun getServerPort(): Int = DEFAULT_PORT

    private fun createMcpServer(): Server {
        val server = Server(
            serverInfo = Implementation(
                name = "coding-aider-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = true),
                    resources = ServerCapabilities.Resources(
                        subscribe = true,
                        listChanged = true
                    )
                )
            )
        )

        // Register MCP tools
        registerPersistentFileTools(server)
        
        return server
    }

    private fun registerPersistentFileTools(server: Server) {
        val persistentFileService = project.service<PersistentFileService>()
        val mcpToolsService = project.service<McpToolsService>()
        
        // Register all persistent file tools
        mcpToolsService.registerPersistentFileTools(server, persistentFileService)
    }

    fun dispose() {
        stopServer()
        serviceScope.cancel()
    }
}
