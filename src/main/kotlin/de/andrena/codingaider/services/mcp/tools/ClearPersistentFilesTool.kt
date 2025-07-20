package de.andrena.codingaider.services.mcp.tools

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.services.mcp.McpTool
import de.andrena.codingaider.services.mcp.McpToolExecutionException
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * MCP tool for clearing all files from the persistent files context
 */
class ClearPersistentFilesTool(private val project: Project) : McpTool {
    
    private val persistentFileService by lazy { project.service<PersistentFileService>() }
    
    override fun getName(): String = "clear_persistent_files"
    
    override fun getDescription(): String = "Clear all files from the persistent files context"
    
    override fun getInputSchema(): Tool.Input = Tool.Input(
        properties = buildJsonObject {
            // No properties needed for this tool
        }
    )
    
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            val currentFiles = persistentFileService.getPersistentFiles()
            val filePaths = currentFiles.map { it.filePath }
            
            persistentFileService.removePersistentFiles(filePaths)
            
            CallToolResult(
                content = listOf(
                    TextContent("Cleared all ${currentFiles.size} files from persistent context")
                )
            )
        } catch (e: Exception) {
            throw McpToolExecutionException(getName(), e.message ?: "Unknown error", e)
        }
    }
}
