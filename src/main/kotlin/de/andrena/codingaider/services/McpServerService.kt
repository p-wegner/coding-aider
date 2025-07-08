package de.andrena.codingaider.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.sse.*
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class McpServerService(private val project: Project) {
    private val logger = Logger.getInstance(McpServerService::class.java)
    private var ktorServer: ApplicationEngine? = null
    private var mcpServer: Server? = null
    private val isRunning = AtomicBoolean(false)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val settings = AiderSettings.getInstance()
    private var currentPort: Int = DEFAULT_PORT
    
    companion object {
        const val DEFAULT_PORT = 8080
        const val MCP_ENDPOINT = "/mcp"
        const val SSE_ENDPOINT = "/sse"
        private const val NOTIFICATION_GROUP_ID = "Coding Aider MCP Server"
    }

    init {
        // Start server automatically if enabled in settings
        if (settings.mcpServerEnabled) {
            startServer(settings.mcpServerPort)
        }
        
        // Listen for settings changes
        settings.addSettingsChangeListener {
            if (settings.mcpServerEnabled && !isRunning.get()) {
                startServer(settings.mcpServerPort)
            } else if (!settings.mcpServerEnabled && isRunning.get()) {
                stopServer()
            } else if (settings.mcpServerEnabled && isRunning.get()) {
                // Restart server if port changed
                val currentPort = getServerPort()
                if (currentPort != settings.mcpServerPort) {
                    stopServer()
                    startServer(settings.mcpServerPort)
                }
            }
        }
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
                
                currentPort = port
                isRunning.set(true)
                logger.info("MCP Server started on port $port")
                
                // Show success notification
                showNotification(
                    "MCP Server Started",
                    "MCP Server is now running on port $port",
                    NotificationType.INFORMATION
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to start MCP Server on port $port", e)
            isRunning.set(false)
            
            // Show error notification
            showNotification(
                "MCP Server Start Failed",
                "Failed to start MCP Server on port $port: ${e.message}",
                NotificationType.ERROR
            )
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
                
                // Show stop notification
                showNotification(
                    "MCP Server Stopped",
                    "MCP Server has been stopped",
                    NotificationType.INFORMATION
                )
            }
        } catch (e: Exception) {
            logger.error("Error stopping MCP Server", e)
            
            // Show error notification
            showNotification(
                "MCP Server Stop Failed",
                "Error stopping MCP Server: ${e.message}",
                NotificationType.ERROR
            )
        }
    }

    fun isServerRunning(): Boolean = isRunning.get()

    fun getServerPort(): Int = currentPort

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

    private fun showNotification(title: String, content: String, type: NotificationType) {
        val notificationGroup = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
        
        notificationGroup.createNotification(title, content, type)
            .notify(project)
    }

    fun dispose() {
        stopServer()
        serviceScope.cancel()
    }
}
