package de.andrena.codingaider.services.mcp.tools

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.mcp.McpTool
import de.andrena.codingaider.services.mcp.McpToolExecutionException
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * MCP tool for getting the current list of persistent files
 */
class GetPersistentFilesTool(private val project: Project) : McpTool {
    
    private val persistentFileService by lazy { project.service<PersistentFileService>() }
    
    override fun getName(): String = "get_persistent_files"
    
    override fun getDescription(): String = "Get the current list of persistent files in the project context"
    
    override fun getInputSchema(): Tool.Input = Tool.Input(
        properties = buildJsonObject {
            // No properties needed for this tool
        }
    )
    
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            val files = persistentFileService.getPersistentFiles()
            val filesJson = files.map { file: FileData ->
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
            throw McpToolExecutionException(getName(), e.message ?: "Unknown error", e)
        }
    }
}
