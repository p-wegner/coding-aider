# [Coding Aider Plan]

# Embedded MCP Webserver for Persistent Files Management

## Overview

This plan outlines the implementation of an embedded MCP (Model Context Protocol) webserver within the Coding-Aider IntelliJ plugin. The webserver will provide HTTP-based access to modify persistent files, allowing external MCP clients to interact with the plugin's persistent file management system. The server lifecycle will be tied to the plugin lifecycle, ensuring proper resource management.

## Problem Description

Currently, the plugin has an `McpServerService` that uses SSE (Server-Sent Events) transport, but there's a need for a more robust HTTP-based MCP server that can handle persistent file operations. The existing implementation has some limitations:

1. The current MCP server implementation uses SSE transport which may not be suitable for all client scenarios
2. The server needs better integration with the plugin's lifecycle management
3. HTTP transport would provide more reliable communication for persistent file operations
4. The server should be easily discoverable and accessible by external MCP clients

## Goals

1. **Implement HTTP Transport**: Replace or complement the existing SSE transport with proper HTTP transport for MCP communication
2. **Plugin Lifecycle Integration**: Ensure the MCP server starts when the plugin loads and stops when the plugin is disposed
3. **Persistent File Management**: Provide MCP tools for managing persistent files (add, remove, list, clear operations)
4. **Robust Error Handling**: Implement proper error handling and logging for server operations
5. **Configuration Management**: Allow configuration of server port and other settings
6. **Resource Management**: Ensure proper cleanup of server resources when the plugin is disposed

## Additional Notes and Constraints

- The server must use HTTP transport instead of or in addition to SSE
- Server lifecycle must be tied to the IntelliJ plugin lifecycle
- The implementation should leverage the existing `PersistentFileService`
- Port configuration should be configurable but have sensible defaults
- The server should be accessible from external MCP clients
- Proper error handling and logging must be implemented
- The implementation should follow IntelliJ plugin development best practices

## References

- Existing `McpServerService` implementation in `src/main/kotlin/de/andrena/codingaider/services/McpServerService.kt`
- MCP Kotlin SDK documentation in `docs/mcp-sdk-docs/USER_GUIDE.md`
- Gradle build configuration in `build.gradle.kts`
- Plugin README.md for context on existing functionality
