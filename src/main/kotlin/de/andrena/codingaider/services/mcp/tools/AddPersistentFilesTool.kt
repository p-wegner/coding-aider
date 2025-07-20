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
 * MCP tool for adding files to the persistent files context
 */
class AddPersistentFilesTool(private val project: Project) : McpTool {
    
    private val persistentFileService by lazy { project.service<PersistentFileService>() }
    
    override fun getName(): String = "add_persistent_files"
    
    override fun getDescription(): String = "Add files to the persistent files context"
    
    override fun getInputSchema(): Tool.Input = Tool.Input(
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
    
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            val filesArray = arguments["files"]?.jsonArray
                ?: throw McpToolArgumentException(getName(), "files parameter is required")
            
            val fileDataList = filesArray.map { fileElement ->
                val fileObj = fileElement.jsonObject
                val filePath = fileObj["filePath"]?.jsonPrimitive?.content
                    ?: throw McpToolArgumentException(getName(), "filePath is required for each file")
                val isReadOnly = fileObj["isReadOnly"]?.jsonPrimitive?.booleanOrNull ?: false
                FileData(filePath, isReadOnly)
            }
            
            persistentFileService.addAllFiles(fileDataList)
            
            CallToolResult(
                content = listOf(
                    TextContent("Added ${fileDataList.size} files to persistent context")
                )
            )
        } catch (e: McpToolArgumentException) {
            throw e
        } catch (e: Exception) {
            throw McpToolExecutionException(getName(), e.message ?: "Unknown error", e)
        }
    }
}
