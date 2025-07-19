package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.settings.AiderSettings
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
    private val persistentFileService by lazy { project.service<PersistentFileService>() }
    private val settings by lazy { AiderSettings.getInstance() }
    private var serverPort = DEFAULT_PORT
    private var httpServer: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    
    // Tool configuration
    private var enableGetPersistentFiles = true
    private var enableAddPersistentFiles = true
    private var enableRemovePersistentFiles = true
    private var enableClearPersistentFiles = true
    
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
                    
                    // Add tools for persistent file management
                    LOG.info("Registering MCP tools for persistent file management...")
                    addPersistentFileTools()
                    LOG.info("Registered 4 MCP tools: get_persistent_files, add_persistent_files, remove_persistent_files, clear_persistent_files")
                    
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
                                val status = buildJsonObject {
                                    put("running", isRunning.get())
                                    put("port", serverPort)
                                    put("project", project.name)
                                    put("persistentFiles", persistentFileService.getPersistentFiles().size)
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
    
    fun updateToolConfiguration(
        enableGetPersistentFiles: Boolean,
        enableAddPersistentFiles: Boolean,
        enableRemovePersistentFiles: Boolean,
        enableClearPersistentFiles: Boolean
    ) {
        this.enableGetPersistentFiles = enableGetPersistentFiles
        this.enableAddPersistentFiles = enableAddPersistentFiles
        this.enableRemovePersistentFiles = enableRemovePersistentFiles
        this.enableClearPersistentFiles = enableClearPersistentFiles
        
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
    
    private fun addPersistentFileTools() {
        mcpServer?.apply {
            var toolCount = 0
            if (enableGetPersistentFiles) {
                addGetPersistentFilesTool()
                toolCount++
            }
            if (enableAddPersistentFiles) {
                addAddPersistentFilesTool()
                toolCount++
            }
            if (enableRemovePersistentFiles) {
                addRemovePersistentFilesTool()
                toolCount++
            }
            if (enableClearPersistentFiles) {
                addClearPersistentFilesTool()
                toolCount++
            }
            LOG.info("Registered $toolCount MCP tools based on configuration")
        }
    }
    
    private fun Server.addGetPersistentFilesTool() {
        addTool(
            name = "get_persistent_files",
            description = "Get the current list of persistent files in the project context",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    // No properties needed for this tool
                }
            )
        ) { request ->
            try {
                val files = persistentFileService.getPersistentFiles()
                val filesJson = files.map { file ->
                    buildJsonObject {
                        put("filePath", file.filePath)
                        put("isReadOnly", file.isReadOnly)
                        put("normalizedPath", file.normalizedFilePath)
                    }
                }
                
                CallToolResult(
                    content = listOf(
                        TextContent("Retrieved ${files.size} persistent files"),
                        TextContent("Files: ${filesJson.joinToString(", ")}")
                    )
                )
            } catch (e: Exception) {
                CallToolResult.error("Failed to retrieve persistent files: ${e.message}")
            }
        }
    }
    
    private fun Server.addAddPersistentFilesTool() {
        addTool(
            name = "add_persistent_files",
            description = "Add files to the persistent files context",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("files") {
                        put("type", "array")
                        putJsonObject("items") {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("filePath") {
                                    put("type", "string")
                                }
                                putJsonObject("isReadOnly") {
                                    put("type", "boolean")
                                    put("default", false)
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("filePath"))
                            }
                        }
                    }
                },
                required = listOf("files")
            )
        ) { request ->
            try {
                val filesArray = request.arguments["files"]?.jsonArray
                val fileDataList = filesArray?.map { fileElement ->
                    val fileObj = fileElement.jsonObject
                    val filePath = fileObj["filePath"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("filePath is required")
                    val isReadOnly = fileObj["isReadOnly"]?.jsonPrimitive?.booleanOrNull ?: false
                    FileData(filePath, isReadOnly)
                } ?: emptyList()
                
                persistentFileService.addAllFiles(fileDataList)
                
                CallToolResult(
                    content = listOf(
                        TextContent("Added ${fileDataList.size} files to persistent context")
                    )
                )
            } catch (e: Exception) {
                CallToolResult.error("Failed to add persistent files: ${e.message}")
            }
        }
    }
    
    private fun Server.addRemovePersistentFilesTool() {
        addTool(
            name = "remove_persistent_files",
            description = "Remove files from the persistent files context",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    putJsonObject("filePaths") {
                        put("type", "array")
                        putJsonObject("items") {
                            put("type", "string")
                        }
                    }
                },
                required = listOf("filePaths")
            )
        ) { request ->
            try {
                val filePathsArray = request.arguments["filePaths"]?.jsonArray
                val filePaths = filePathsArray?.map { it.jsonPrimitive.content } ?: emptyList()
                
                persistentFileService.removePersistentFiles(filePaths)
                
                CallToolResult(
                    content = listOf(
                        TextContent("Removed ${filePaths.size} files from persistent context")
                    )
                )
            } catch (e: Exception) {
                CallToolResult.error("Failed to remove persistent files: ${e.message}")
            }
        }
    }
    
    private fun Server.addClearPersistentFilesTool() {
        addTool(
            name = "clear_persistent_files",
            description = "Clear all files from the persistent files context",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    // No properties needed for this tool
                }
            )
        ) { request ->
            try {
                val currentFiles = persistentFileService.getPersistentFiles()
                val filePaths = currentFiles.map { it.filePath }
                
                persistentFileService.removePersistentFiles(filePaths)
                
                CallToolResult(
                    content = listOf(
                        TextContent("Cleared all ${currentFiles.size} files from persistent context")
                    )
                )
            } catch (e: Exception) {
                CallToolResult.error("Failed to clear persistent files: ${e.message}")
            }
        }
    }
    
    fun dispose() {
        LOG.info("Disposing MCP server service")
        stopServer()
        coroutineScope.cancel()
    }
}
