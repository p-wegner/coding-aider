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
import java.util.concurrent.atomic.AtomicBoolean
import de.andrena.codingaider.settings.AiderSettings
import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

@Service(Service.Level.PROJECT)
class McpServerService(private val project: Project) {
    private val logger = Logger.getInstance(McpServerService::class.java)
    private var ktorServer: ApplicationEngine? = null
    private var mcpServer: Server? = null
    private val isRunning = AtomicBoolean(false)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MCP-Server-${project.name}").apply {
            isDaemon = true
        }
    }
    private val settings = AiderSettings.getInstance()
    private var currentPort: Int = DEFAULT_PORT
    
    companion object {
        const val DEFAULT_PORT = 8080
        const val MCP_ENDPOINT = "/mcp"
        const val SSE_ENDPOINT = "/sse"
        private const val NOTIFICATION_GROUP_ID = "Coding Aider MCP Server"
    }

    init {
        logger.info("McpServerService initialized for project: ${project.name}")
        logger.info("MCP Server enabled in settings: ${settings.mcpServerEnabled}")
        logger.info("MCP Server port in settings: ${settings.mcpServerPort}")
        
        // Start server automatically if enabled in settings
        if (settings.mcpServerEnabled) {
            logger.info("Starting MCP Server automatically on initialization")
            startServer(settings.mcpServerPort)
        }
        
        // Listen for settings changes
        settings.addSettingsChangeListener {
            logger.info("Settings changed - MCP enabled: ${settings.mcpServerEnabled}, running: ${isRunning.get()}")
            if (settings.mcpServerEnabled && !isRunning.get()) {
                logger.info("Starting MCP Server due to settings change")
                startServer(settings.mcpServerPort)
            } else if (!settings.mcpServerEnabled && isRunning.get()) {
                logger.info("Stopping MCP Server due to settings change")
                stopServer()
            } else if (settings.mcpServerEnabled && isRunning.get()) {
                // Restart server if port changed
                val currentPort = getServerPort()
                if (currentPort != settings.mcpServerPort) {
                    logger.info("Restarting MCP Server due to port change: $currentPort -> ${settings.mcpServerPort}")
                    stopServer()
                    startServer(settings.mcpServerPort)
                }
            }
        }
    }

    fun startServer(port: Int = DEFAULT_PORT) {
        if (isRunning.get()) {
            logger.warn("MCP Server is already running on port $currentPort")
            return
        }

        logger.info("Attempting to start MCP Server on port $port")
        
        executor.submit {
            try {
                logger.info("Creating MCP Server instance")
                mcpServer = createMcpServer()
                logger.info("MCP Server instance created successfully")
                
                logger.info("Starting Ktor embedded server on port $port")
                ktorServer = embeddedServer(Netty, port = port) {
                    install(SSE)
                    mcp {
                        mcpServer!!
                    }
                }
                
                // Start server without blocking
                ktorServer!!.start(wait = false)
                
                currentPort = port
                isRunning.set(true)
                logger.info("MCP Server successfully started on port $port")
                
                // Show success notification
                ApplicationManager.getApplication().invokeLater {
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
                ApplicationManager.getApplication().invokeLater {
                    showNotification(
                        "MCP Server Start Failed",
                        "Failed to start MCP Server on port $port: ${e.message}",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun stopServer() {
        if (!isRunning.get()) {
            return
        }

        executor.submit {
            try {
                ktorServer?.stop(1000, 2000)
                ktorServer = null
                mcpServer = null
                isRunning.set(false)
                logger.info("MCP Server stopped")
                
                // Show stop notification
                ApplicationManager.getApplication().invokeLater {
                    showNotification(
                        "MCP Server Stopped",
                        "MCP Server has been stopped",
                        NotificationType.INFORMATION
                    )
                }
            } catch (e: Exception) {
                logger.error("Error stopping MCP Server", e)
                
                // Show error notification
                ApplicationManager.getApplication().invokeLater {
                    showNotification(
                        "MCP Server Stop Failed",
                        "Error stopping MCP Server: ${e.message}",
                        NotificationType.ERROR
                    )
                }
            }
        }
    }

    fun isServerRunning(): Boolean = isRunning.get()

    fun getServerPort(): Int = currentPort

    private fun createMcpServer(): Server {
        logger.info("Creating MCP Server with capabilities")
        
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

        logger.info("MCP Server instance created, registering tools")
        
        // Register MCP tools
        registerPersistentFileTools(server)
        
        logger.info("MCP tools registered successfully")
        return server
    }

    private fun registerPersistentFileTools(server: Server) {
        logger.info("Getting PersistentFileService and McpToolsService")
        val persistentFileService = project.service<PersistentFileService>()
        val mcpToolsService = project.service<McpToolsService>()
        
        logger.info("Registering persistent file tools")
        // Register all persistent file tools
        mcpToolsService.registerPersistentFileTools(server, persistentFileService)
        logger.info("Persistent file tools registered")
    }

    private fun showNotification(title: String, content: String, type: NotificationType) {
        try {
            // Try to get the notification group, if it doesn't exist, create a simple notification
            val notificationGroup = try {
                NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
            } catch (e: Exception) {
                logger.warn("Could not get notification group, will create simple notification", e)
                null
            }
            
            if (notificationGroup != null) {
                notificationGroup.createNotification(title, content, type)
                    .notify(project)
                logger.info("Notification shown: $title - $content")
            } else {
                // Fallback: create a simple notification without group
                logger.info("Using fallback notification: $title - $content")
                // Just log the notification since we can't show it properly
                logger.info("MCP Server notification: $title - $content")
            }
        } catch (e: Exception) {
            logger.error("Failed to show notification: $title", e)
        }
    }

    fun dispose() {
        stopServer()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
