package de.andrena.codingaider.services.mcp

/**
 * Base exception class for MCP tool errors
 */
open class McpToolException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when a tool fails to execute
 */
class McpToolExecutionException(toolName: String, message: String, cause: Throwable? = null) : 
    McpToolException("Tool '$toolName' execution failed: $message", cause)

/**
 * Exception thrown when tool arguments are invalid
 */
class McpToolArgumentException(toolName: String, message: String) : 
    McpToolException("Tool '$toolName' has invalid arguments: $message")

/**
 * Exception thrown when a tool is not found
 */
class McpToolNotFoundException(toolName: String) : 
    McpToolException("Tool '$toolName' not found in registry")
