package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import de.andrena.codingaider.command.FileData
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.*
import kotlinx.serialization.json.*

@Service(Service.Level.PROJECT)
class McpToolsService(private val project: Project) {
    private val logger = Logger.getInstance(McpToolsService::class.java)

    fun registerPersistentFileTools(server: Server, persistentFileService: PersistentFileService) {
        registerListPersistentFilesTool(server, persistentFileService)
        registerAddPersistentFilesTool(server, persistentFileService)
        registerRemovePersistentFilesTool(server, persistentFileService)
        registerGetPersistentFileContentTool(server, persistentFileService)
    }

    private fun registerListPersistentFilesTool(server: Server, persistentFileService: PersistentFileService) {
        server.addTool(
            name = "list_persistent_files",
            description = "Returns all files currently in the persistent file list",
            inputSchema = Tool.Input(
                type = "object",
                properties = emptyMap()
            )
        ) { request ->
            try {
                val persistentFiles = persistentFileService.getPersistentFiles()
                val filesJson = persistentFiles.map { fileData ->
                    buildJsonObject {
                        put("path", fileData.filePath)
                        put("isReadOnly", fileData.isReadOnly)
                        put("normalizedPath", fileData.normalizedFilePath)
                    }
                }
                
                val result = buildJsonObject {
                    put("files", JsonArray(filesJson))
                    put("count", persistentFiles.size)
                }
                
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text",
                            text = result.toString()
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Error listing persistent files", e)
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text", 
                            text = "Error: ${e.message}"
                        )
                    ),
                    isError = true
                )
            }
        }
    }

    private fun registerAddPersistentFilesTool(server: Server, persistentFileService: PersistentFileService) {
        server.addTool(
            name = "add_persistent_files",
            description = "Adds one or more files to the persistent file list",
            inputSchema = Tool.Input(
                type = "object",
                properties = mapOf(
                    "filePaths" to JsonObject(mapOf(
                        "type" to JsonPrimitive("array"),
                        "items" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                        "description" to JsonPrimitive("Array of file paths to add")
                    )),
                    "isReadOnly" to JsonObject(mapOf(
                        "type" to JsonPrimitive("boolean"),
                        "description" to JsonPrimitive("Whether files should be marked as read-only"),
                        "default" to JsonPrimitive(false)
                    ))
                ),
                required = listOf("filePaths")
            )
        ) { request ->
            try {
                val filePaths = request.arguments["filePaths"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: throw IllegalArgumentException("filePaths parameter is required")
                
                val isReadOnly = request.arguments["isReadOnly"]?.jsonPrimitive?.booleanOrNull ?: false
                
                val addedFiles = mutableListOf<FileData>()
                filePaths.forEach { filePath ->
                    val fileData = FileData(filePath, isReadOnly)
                    persistentFileService.addFile(fileData)
                    addedFiles.add(fileData)
                }
                
                val result = buildJsonObject {
                    put("success", true)
                    put("addedCount", addedFiles.size)
                    put("addedFiles", JsonArray(addedFiles.map { 
                        buildJsonObject {
                            put("path", it.filePath)
                            put("isReadOnly", it.isReadOnly)
                        }
                    }))
                }
                
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text",
                            text = result.toString()
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Error adding persistent files", e)
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text",
                            text = "Error: ${e.message}"
                        )
                    ),
                    isError = true
                )
            }
        }
    }

    private fun registerRemovePersistentFilesTool(server: Server, persistentFileService: PersistentFileService) {
        server.addTool(
            name = "remove_persistent_files",
            description = "Removes files from the persistent file list",
            inputSchema = Tool.Input(
                type = "object",
                properties = mapOf(
                    "filePaths" to JsonObject(mapOf(
                        "type" to JsonPrimitive("array"),
                        "items" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                        "description" to JsonPrimitive("Array of file paths to remove")
                    ))
                ),
                required = listOf("filePaths")
            )
        ) { request ->
            try {
                val filePaths = request.arguments["filePaths"]?.jsonArray?.map { it.jsonPrimitive.content }
                    ?: throw IllegalArgumentException("filePaths parameter is required")
                
                persistentFileService.removePersistentFiles(filePaths)
                
                val result = buildJsonObject {
                    put("success", true)
                    put("removedCount", filePaths.size)
                    put("removedFiles", JsonArray(filePaths.map { JsonPrimitive(it) }))
                }
                
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text",
                            text = result.toString()
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Error removing persistent files", e)
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text",
                            text = "Error: ${e.message}"
                        )
                    ),
                    isError = true
                )
            }
        }
    }

    private fun registerGetPersistentFileContentTool(server: Server, persistentFileService: PersistentFileService) {
        server.addTool(
            name = "get_persistent_file_content",
            description = "Retrieves the content of a persistent file",
            inputSchema = Tool.Input(
                type = "object",
                properties = mapOf(
                    "filePath" to JsonObject(mapOf(
                        "type" to JsonPrimitive("string"),
                        "description" to JsonPrimitive("Path of the file to read")
                    ))
                ),
                required = listOf("filePath")
            )
        ) { request ->
            try {
                val filePath = request.arguments["filePath"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("filePath parameter is required")
                
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
                    ?: throw IllegalArgumentException("File not found: $filePath")
                
                if (!virtualFile.exists()) {
                    throw IllegalArgumentException("File does not exist: $filePath")
                }
                
                val content = String(virtualFile.contentsToByteArray())
                
                val result = buildJsonObject {
                    put("filePath", filePath)
                    put("content", content)
                    put("size", content.length)
                    put("mimeType", virtualFile.fileType.name)
                }
                
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text",
                            text = result.toString()
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Error reading persistent file content", e)
                CallToolResult(
                    content = listOf(
                        TextContent(
                            type = "text",
                            text = "Error: ${e.message}"
                        )
                    ),
                    isError = true
                )
            }
        }
    }
}
