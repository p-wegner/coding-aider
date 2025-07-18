package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class McpServerService(private val project: Project) {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)
    private var mcpServer: Server? = null
    private val persistentFileService by lazy { project.service<PersistentFileService>() }
    private val serverPort = 8080 // Default port, could be configurable
    
    init {
        // Start the MCP server automatically when the service is created
        startServer()
    }
    
    fun startServer() {
        if (isRunning.compareAndSet(false, true)) {
            coroutineScope.launch {
                try {
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
                    addPersistentFileTools()
                    
                    // Connect with HTTP SSE transport
                    val transport = SseServerTransport(
                        endpoint = "/sse",
                        port = serverPort
                    )
                    mcpServer?.connect(transport)
                    
                    isRunning.set(true)
                } catch (e: Exception) {
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
                mcpServer?.close()
                mcpServer = null
            }
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
        stopServer()
        coroutineScope.cancel()
    }
}
