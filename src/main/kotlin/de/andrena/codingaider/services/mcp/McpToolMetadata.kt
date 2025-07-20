package de.andrena.codingaider.services.mcp

import io.modelcontextprotocol.kotlin.sdk.Tool

/**
 * Metadata about an MCP tool for configuration and display purposes
 */
data class McpToolMetadata(
    val name: String,
    val description: String,
    val inputSchema: Tool.Input,
    val isEnabledByDefault: Boolean = true,
    val category: String = "General"
)
