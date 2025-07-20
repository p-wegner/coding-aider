package de.andrena.codingaider.services.mcp

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonObject

/**
 * Interface for MCP tools that can be dynamically discovered and registered
 */
interface McpTool {
    /**
     * Get the unique name of this tool
     */
    fun getName(): String
    
    /**
     * Get a human-readable description of what this tool does
     */
    fun getDescription(): String
    
    /**
     * Get the JSON schema that defines the input parameters for this tool
     */
    fun getInputSchema(): Tool.Input
    
    /**
     * Execute the tool with the given arguments
     * @param arguments The arguments passed to the tool as a JsonObject
     * @return The result of the tool execution
     */
    suspend fun execute(arguments: JsonObject): CallToolResult
    
    /**
     * Whether this tool is enabled by default
     */
    fun isEnabledByDefault(): Boolean = true
    
    /**
     * Get metadata for this tool
     */
    fun getMetadata(): McpToolMetadata = McpToolMetadata(
        name = getName(),
        description = getDescription(),
        inputSchema = getInputSchema(),
        isEnabledByDefault = isEnabledByDefault()
    )
}
