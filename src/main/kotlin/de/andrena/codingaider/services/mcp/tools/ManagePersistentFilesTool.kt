package de.andrena.codingaider.services.mcp.tools

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.mcp.McpTool
import de.andrena.codingaider.services.mcp.McpToolArgumentException
import de.andrena.codingaider.services.mcp.McpToolExecutionException
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * MCP tool for managing persistent files context with multiple operations
 */
class ManagePersistentFilesTool(private val project: Project) : McpTool {
    
    private val persistentFileService by lazy { project.service<PersistentFileService>() }
    
    override fun getName(): String = "manage_persistent_files"
    
    override fun getDescription(): String = 
        "Manage persistent files context. Actions: 'get' (list current files), " +
        "'add' (add files), 'remove' (remove specific files), 'clear' (remove all files)"
    
    override fun getInputSchema(): Tool.Input = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("action") {
                put("type", "string")
                put("description", "Action to perform: get, add, remove, or clear")
                putJsonArray("enum") {
                    add(JsonPrimitive("get"))
                    add(JsonPrimitive("add"))
                    add(JsonPrimitive("remove"))
                    add(JsonPrimitive("clear"))
                }
            }
            putJsonObject("files") {
                put("type", "array")
                put("description", "Files to add (for 'add' action)")
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
            putJsonObject("filePaths") {
                put("type", "array")
                put("description", "File paths to remove (for 'remove' action)")
                putJsonObject("items") {
                    put("type", "string")
                }
            }
        },
        required = listOf("action")
    )
    
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            val action = arguments["action"]?.jsonPrimitive?.content
                ?: throw McpToolArgumentException(getName(), "action parameter is required")
            
            when (action) {
                "get" -> handleGetAction()
                "add" -> handleAddAction(arguments)
                "remove" -> handleRemoveAction(arguments)
                "clear" -> handleClearAction()
                else -> throw McpToolArgumentException(getName(), "Invalid action: $action. Must be one of: get, add, remove, clear")
            }
        } catch (e: McpToolArgumentException) {
            throw e
        } catch (e: Exception) {
            throw McpToolExecutionException(getName(), e.message ?: "Unknown error", e)
        }
    }
    
    private fun handleGetAction(): CallToolResult {
        val files = persistentFileService.getPersistentFiles()
        val filesJson = files.map { file: FileData ->
            buildJsonObject {
                put("filePath", JsonPrimitive(file.filePath))
                put("isReadOnly", JsonPrimitive(file.isReadOnly))
                put("normalizedPath", JsonPrimitive(file.normalizedFilePath))
            }
        }
        
        return CallToolResult(
            content = listOf(
                TextContent("Retrieved ${files.size} persistent files"),
                TextContent("Files: ${filesJson.joinToString(", ")}")
            )
        )
    }
    
    private fun handleAddAction(arguments: JsonObject): CallToolResult {
        val filesArray = arguments["files"]?.jsonArray
            ?: throw McpToolArgumentException(getName(), "files parameter is required for 'add' action")
        
        val fileDataList = filesArray.map { fileElement ->
            val fileObj = fileElement.jsonObject
            val filePath = fileObj["filePath"]?.jsonPrimitive?.content
                ?: throw McpToolArgumentException(getName(), "filePath is required for each file")
            val isReadOnly = fileObj["isReadOnly"]?.jsonPrimitive?.booleanOrNull ?: false
            FileData(filePath, isReadOnly)
        }
        
        persistentFileService.addAllFiles(fileDataList)
        
        return CallToolResult(
            content = listOf(
                TextContent("Added ${fileDataList.size} files to persistent context")
            )
        )
    }
    
    private fun handleRemoveAction(arguments: JsonObject): CallToolResult {
        val filePathsArray = arguments["filePaths"]?.jsonArray
            ?: throw McpToolArgumentException(getName(), "filePaths parameter is required for 'remove' action")
        
        val filePaths = filePathsArray.map { it.jsonPrimitive.content }
        
        persistentFileService.removePersistentFiles(filePaths)
        
        return CallToolResult(
            content = listOf(
                TextContent("Removed ${filePaths.size} files from persistent context")
            )
        )
    }
    
    private fun handleClearAction(): CallToolResult {
        val currentFiles = persistentFileService.getPersistentFiles()
        val filePaths = currentFiles.map { it.filePath }
        
        persistentFileService.removePersistentFiles(filePaths)
        
        return CallToolResult(
            content = listOf(
                TextContent("Cleared all ${currentFiles.size} files from persistent context")
            )
        )
    }
}
