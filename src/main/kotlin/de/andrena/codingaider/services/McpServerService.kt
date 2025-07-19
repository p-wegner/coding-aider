package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.io.buffered
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
    private var serverPort = DEFAULT_PORT
    private var httpServer: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val messageChannel = Channel<String>(Channel.UNLIMITED)
    private var serverTransport: StdioServerTransport? = null
    
    init {
        LOG.info("Initializing MCP Server Service for project: ${project.name}")
        // Start the MCP server automatically when the service is created
        startServer()
    }
    
    fun startServer() {
        if (isRunning.compareAndSet(false, true)) {
            coroutineScope.launch {
                try {
                    LOG.info("=== MCP Server Startup ===")
                    LOG.info("Starting MCP server for project: ${project.name}")
                    LOG.info("Server name: coding-aider-persistent-files")
                    LOG.info("Server version: 1.0.0")
                    
                    // Find available port
                    serverPort = findAvailablePort(DEFAULT_PORT)
                    LOG.info("Found available port: $serverPort")
                    
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
                    
                    // Create pipes for STDIO transport over HTTP
                    val inputPipe = PipedInputStream()
                    val outputPipe = PipedOutputStream()
                    val outputInputStream = PipedInputStream()
                    val inputOutputStream = PipedOutputStream(inputPipe)
                    outputPipe.connect(outputInputStream)
                    
                    // Create STDIO transport
                    serverTransport = StdioServerTransport(
                        inputStream = inputPipe.asSource().buffered(),
                        outputStream = outputPipe.asSink().buffered()
                    )
                    
                    // Start HTTP server with MCP over HTTP
                    httpServer = embeddedServer(CIO, host = "0.0.0.0", port = serverPort) {
                        routing {
                            post("/mcp") {
                                try {
                                    val requestBody = call.receiveText()
                                    LOG.debug("Received MCP request: $requestBody")
                                    
                                    // Write request to input stream
                                    inputOutputStream.write(requestBody.toByteArray())
                                    inputOutputStream.write('\n'.code)
                                    inputOutputStream.flush()
                                    
                                    // Read response from output stream
                                    val buffer = ByteArray(8192)
                                    val bytesRead = outputInputStream.read(buffer)
                                    val response = if (bytesRead > 0) {
                                        String(buffer, 0, bytesRead)
                                    } else {
                                        "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal error\"}}"
                                    }
                                    
                                    LOG.debug("Sending MCP response: $response")
                                    call.respondText(response, ContentType.Application.Json)
                                } catch (e: Exception) {
                                    LOG.error("Error processing MCP request", e)
                                    call.respond(
                                        HttpStatusCode.InternalServerError,
                                        "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"${e.message}\"}}"
                                    )
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
                    
                    // Connect MCP server to transport
                    LOG.info("Connecting MCP server to STDIO transport...")
                    mcpServer?.connect(serverTransport!!)
                    
                    LOG.info("=== MCP Server Started Successfully ===")
                    LOG.info("HTTP endpoint: http://localhost:$serverPort/mcp")
                    LOG.info("Health check: http://localhost:$serverPort/health")
                    LOG.info("Status endpoint: http://localhost:$serverPort/status")
                    LOG.info("MCP clients can now connect to: http://localhost:$serverPort/mcp")
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
                    serverTransport = null
                    httpServer?.stop(1000, 2000)
                    httpServer = null
                    messageChannel.close()
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
    
    private fun addPersistentFileTools() {
        mcpServer?.apply {
            addGetPersistentFilesTool()
            addAddPersistentFilesTool()
            addRemovePersistentFilesTool()
            addClearPersistentFilesTool()
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
