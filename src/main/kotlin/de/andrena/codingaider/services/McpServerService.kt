package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.services.mcp.McpToolRegistry
import de.andrena.codingaider.services.PersistentFileService
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean
import java.net.ServerSocket
import java.io.IOException

@Service(Service.Level.PROJECT)
class McpServerService(private val project: Project) {
    
    companion object {
        private val LOG = Logger.getInstance(McpServerService::class.java)
        private const val DEFAULT_PORT = 8080
        private const val MAX_PORT_ATTEMPTS = 10
    }
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)
    private var mcpServer: Server? = null
    private val settings by lazy { AiderSettings.getInstance() }
    private val mcpToolRegistry by lazy { McpToolRegistry.getInstance(project) }
    private var serverPort = DEFAULT_PORT
    private var httpServer: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    
    init {
        LOG.info("Initializing MCP Server Service for project: ${project.name}")
        // Start the MCP server automatically if enabled and auto-start is configured
        if (settings.enableMcpServer && settings.mcpServerAutoStart) {
            startServer()
        }
    }
    
    fun startServer() {
        if (!settings.enableMcpServer) {
            LOG.info("MCP Server is disabled in settings")
            return
        }
        
        if (isRunning.compareAndSet(false, true)) {
            coroutineScope.launch {
                try {
                    LOG.info("=== MCP Server Startup ===")
                    LOG.info("Starting MCP server for project: ${project.name}")
                    LOG.info("Server name: coding-aider-persistent-files")
                    LOG.info("Server version: 1.0.0")
                    
                    // Find available port starting from configured port
                    val configuredPort = settings.mcpServerPort
                    serverPort = findAvailablePort(configuredPort)
                    LOG.info("Found available port: $serverPort (configured: $configuredPort)")
                    
                    // Initialize MCP server
                    mcpServer = Server(
                        serverInfo = Implementation(
                            name = "coding-aider-persistent-files",
                            version = "1.0.0"
                        ),
                        options = ServerOptions(
                            capabilities = ServerCapabilities(
                                tools = ServerCapabilities.Tools(listChanged = true)
                            )
                        )
                    )
                    
                    // Add tools using the tool registry
                    LOG.info("Discovering and registering MCP tools...")
                    registerDiscoveredTools()
                    val toolCount = mcpToolRegistry.getAvailableTools().size
                    val toolNames = mcpToolRegistry.getAvailableTools().map { it.name }
                    LOG.info("Registered $toolCount MCP tools: ${toolNames.joinToString(", ")}")
                    
                    // Start HTTP server with SSE transport for MCP
                    httpServer = embeddedServer(CIO, host = "0.0.0.0", port = serverPort) {
                        install(SSE)
                        routing {
                            var sseTransport: SseServerTransport? = null
                            
                            sse("/sse") {
                                sseTransport = SseServerTransport("/message", this)
                                LOG.info("Connecting MCP server to SSE transport...")
                                mcpServer?.connect(sseTransport!!)
                            }
                            
                            post("/message") {
                                // Handle POST messages for SSE transport
                                val sessionId = call.request.queryParameters["sessionId"]
                                if (sessionId != null && sseTransport != null) {
                                    val body = call.receiveText()
                                    sseTransport!!.handleMessage(body)
                                    call.respond(HttpStatusCode.OK)
                                } else {
                                    call.respond(HttpStatusCode.BadRequest, "Missing sessionId or SSE transport not initialized")
                                }
                            }
                            
                            get("/health") {
                                call.respondText("MCP Server is running on port $serverPort", ContentType.Text.Plain)
                            }
                            
                            get("/status") {
                                val persistentFileService = project.service<PersistentFileService>()
                                val status = buildJsonObject {
                                    put("running", isRunning.get())
                                    put("port", serverPort)
                                    put("project", project.name)
                                    put("persistentFiles", persistentFileService.getPersistentFiles().size)
                                    put("availableTools", mcpToolRegistry.getToolCount())
                                    put("enabledTools", mcpToolRegistry.getEnabledToolCount())
                                }
                                call.respondText(status.toString(), ContentType.Application.Json)
                            }
                        }
                    }
                    LOG.info("Starting HTTP server on 0.0.0.0:$serverPort...")
                    httpServer?.start(wait = false)
                    
                    LOG.info("=== MCP Server Started Successfully ===")
                    LOG.info("SSE endpoint: http://localhost:$serverPort/sse")
                    LOG.info("Health check: http://localhost:$serverPort/health")
                    LOG.info("Status endpoint: http://localhost:$serverPort/status")
                    LOG.info("MCP clients can now connect to: http://localhost:$serverPort/sse")
                    LOG.info("==========================================")
                    isRunning.set(true)
                } catch (e: Exception) {
                    LOG.error("Failed to start MCP server", e)
                    isRunning.set(false)
                    mcpServer = null
                    throw e
                }
            }
        }
    }
    
    fun stopServer() {
        if (isRunning.compareAndSet(true, false)) {
            coroutineScope.launch {
                try {
                    LOG.info("=== MCP Server Shutdown ===")
                    LOG.info("Stopping MCP server on port $serverPort for project: ${project.name}")
                    mcpServer?.close()
                    mcpServer = null
                    httpServer?.stop(1000, 2000)
                    httpServer = null
                    LOG.info("=== MCP server stopped successfully ===")
                } catch (e: Exception) {
                    LOG.error("Error stopping MCP server", e)
                }
            }
        }
    }
    
    private fun findAvailablePort(startPort: Int): Int {
        for (i in 0 until MAX_PORT_ATTEMPTS) {
            val port = startPort + i
            try {
                ServerSocket(port).use { 
                    return port
                }
            } catch (e: IOException) {
                // Port is in use, try next one
                continue
            }
        }
        throw IOException("Could not find available port starting from $startPort")
    }
    
    fun getServerPort(): Int = serverPort
    
    fun isServerRunning(): Boolean = isRunning.get()
    
    fun getServerStatus(): String {
        return if (isRunning.get()) {
            "Running on port $serverPort"
        } else {
            "Stopped"
        }
    }
    
    fun updateToolConfiguration(toolConfigurations: Map<String, Boolean>) {
        mcpToolRegistry.updateToolConfiguration(toolConfigurations)
        
        // If server is running, restart it to apply new tool configuration
        if (isRunning.get()) {
            LOG.info("Restarting MCP server to apply new tool configuration")
            stopServer()
            // Small delay to ensure server stops completely
            coroutineScope.launch {
                kotlinx.coroutines.delay(500)
                startServer()
            }
        }
    }
    
    fun getAvailableTools() = mcpToolRegistry.getAvailableTools()
    
    fun getEnabledTools() = mcpToolRegistry.getEnabledTools()
    
    private fun registerDiscoveredTools() {
        mcpServer?.apply {
            val enabledTools = mcpToolRegistry.getEnabledTools()
            var registeredCount = 0
            
            enabledTools.forEach { tool ->
                try {
                    val metadata = tool.getMetadata()
                    addTool(
                        name = metadata.name,
                        description = metadata.description,
                        inputSchema = metadata.inputSchema
                    ) { request ->
                        tool.execute(request.arguments ?: kotlinx.serialization.json.buildJsonObject {})
                    }
                    registeredCount++
                    LOG.debug("Registered MCP tool: ${metadata.name}")
                } catch (e: Exception) {
                    LOG.error("Failed to register MCP tool: ${tool.getMetadata().name}", e)
                }
            }
            
            LOG.info("Successfully registered $registeredCount out of ${enabledTools.size} enabled MCP tools")
        }
    }
    
    fun dispose() {
        LOG.info("Disposing MCP server service")
        stopServer()
        coroutineScope.cancel()
    }
}
