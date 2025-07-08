# [Coding Aider Plan] MCP Server Integration

## Overview

This plan outlines the integration of Model Context Protocol (MCP) server functionality into the Coding-Aider IntelliJ plugin. The implementation will start with MCP tools to manage persistent files, allowing external MCP clients (like Cline, VS Code extensions, or Claude) to interact with the plugin's persistent file management system via HTTP/SSE endpoints.

## Problem Description

Currently, the Coding-Aider plugin manages persistent files through its internal UI and services. External tools and AI assistants cannot directly interact with this file management system. By implementing MCP server functionality, we can:

1. Expose persistent file management as standardized MCP tools
2. Allow external MCP clients to query, add, remove, and manage persistent files
3. Enable better integration with AI coding assistants that support MCP
4. Provide a foundation for future MCP tool expansions

## Goals

### Primary Goals
1. **MCP Server Infrastructure**: Implement an embedded HTTP/SSE MCP server within the IntelliJ plugin
2. **Persistent File Tools**: Create MCP tools for managing persistent files:
   - `list_persistent_files`: List all currently persistent files
   - `add_persistent_files`: Add files to the persistent file list
   - `remove_persistent_files`: Remove files from the persistent file list
   - `get_persistent_file_content`: Retrieve content of persistent files
3. **Client Discovery**: Provide configuration and discovery mechanisms for MCP clients
4. **Integration**: Seamlessly integrate with existing PersistentFileService

### Secondary Goals
1. **Stash Management Tools**: Extend MCP tools to handle file stashing operations
2. **Configuration Management**: Allow MCP clients to query and modify plugin settings
3. **Project Context Tools**: Provide tools to query project structure and context

## Implementation Approach

### Phase 1: Core MCP Server Setup
1. Add Kotlin MCP SDK dependencies to build.gradle.kts
2. Create MCP server service that starts an embedded HTTP/SSE server
3. Implement basic server lifecycle management (start/stop with plugin)

### Phase 2: Persistent File MCP Tools
1. Create MCP tool implementations for persistent file operations
2. Integrate tools with existing PersistentFileService
3. Implement proper error handling and validation

### Phase 3: Client Configuration
1. Create configuration files for popular MCP clients (Cline, etc.)
2. Implement server discovery mechanisms
3. Add plugin settings for MCP server configuration

## Technical Details

### Dependencies
- Add Kotlin MCP SDK to build.gradle.kts
- Ktor for HTTP/SSE server functionality (already present)
- kotlinx.serialization for JSON handling (already present)

### Architecture
```
IntelliJ Plugin
├── McpServerService (new)
│   ├── HTTP/SSE Server
│   └── Tool Registry
├── MCP Tools (new)
│   ├── PersistentFileTools
│   └── Future tool categories
└── Existing Services
    ├── PersistentFileService
    └── Other services
```

### MCP Tools Specification

#### list_persistent_files
- **Description**: Returns all files currently in the persistent file list
- **Parameters**: None
- **Returns**: Array of file objects with path, type, and metadata

#### add_persistent_files
- **Description**: Adds one or more files to the persistent file list
- **Parameters**: Array of file paths
- **Returns**: Success status and added file count

#### remove_persistent_files
- **Description**: Removes files from the persistent file list
- **Parameters**: Array of file paths to remove
- **Returns**: Success status and removed file count

#### get_persistent_file_content
- **Description**: Retrieves the content of a persistent file
- **Parameters**: File path
- **Returns**: File content as text

## Additional Notes and Constraints

### Constraints
1. **IntelliJ Threading**: All file operations must respect IntelliJ's threading model
2. **Security**: MCP server should only be accessible locally by default
3. **Performance**: File operations should not block the IDE
4. **Compatibility**: Must work with existing persistent file functionality

### Configuration
- Default MCP server port: 8080 (configurable)
- Server endpoints: `/mcp` for HTTP, `/sse` for Server-Sent Events
- Configuration file location: `.coding-aider-mcp-config.json` in project root

### Error Handling
- Graceful handling of file system errors
- Proper MCP error responses for invalid operations
- Logging of MCP server activities

## References

- [Model Context Protocol Specification](https://modelcontextprotocol.io)
- [Kotlin MCP SDK Documentation](kotlin-sdk/kotlin_mcp_sdk.md)
- [MCP Server Support Documentation](.coding-aider-docs/mcp_server_support.md)
- [Existing PersistentFileService](src/main/kotlin/de/andrena/codingaider/services/PersistentFileService.kt)
