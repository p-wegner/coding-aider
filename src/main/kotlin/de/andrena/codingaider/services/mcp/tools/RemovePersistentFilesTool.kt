package de.andrena.codingaider.services.mcp.tools

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.mcp.McpTool
import de.andrena.codingaider.services.mcp.McpToolArgumentException
import de.andrena.codingaider.services.mcp.McpToolExecutionException
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonObject

/**
 * MCP tool for removing files from the persistent files context
 */
class RemovePersistentFilesTool(private val project: Project) : McpTool {
    
    private val persistentFileService by lazy { project.service<PersistentFileService>() }
    
    override fun getName(): String = "remove_persistent_files"
    
    override fun getDescription(): String = "Remove files from the persistent files context"
    
    override fun getInputSchema(): Tool.Input = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("filePaths") {
                put("type", JsonPrimitive("array"))
                putJsonObject("items") {
                    put("type", JsonPrimitive("string"))
                }
            }
        },
        required = listOf("filePaths")
    )
    
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            val filePathsArray = arguments["filePaths"]?.jsonArray
                ?: throw McpToolArgumentException(getName(), "filePaths parameter is required")
            
            val filePaths = filePathsArray.map { it.jsonPrimitive.content }
            
            persistentFileService.removePersistentFiles(filePaths)
            
            CallToolResult(
                content = listOf(
                    TextContent("Removed ${filePaths.size} files from persistent context")
                )
            )
        } catch (e: McpToolArgumentException) {
            throw e
        } catch (e: Exception) {
            throw McpToolExecutionException(getName(), e.message ?: "Unknown error", e)
        }
    }
}
