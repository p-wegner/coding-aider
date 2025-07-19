# [Coding Aider Plan - Checklist]

# Embedded MCP Webserver Implementation Checklist

## Analysis and Planning
- [x] Review existing McpServerService implementation
- [x] Analyze MCP Kotlin SDK HTTP transport capabilities
- [x] Identify required changes to plugin lifecycle management
- [x] Plan server configuration options

## Core Implementation
- [x] Modify McpServerService to use HTTP transport instead of SSE
- [x] Implement proper HTTP server setup using embedded server
- [x] Configure MCP server with HTTP transport
- [x] Ensure server starts automatically with plugin initialization
- [x] Implement proper server shutdown in dispose method
- [x] Add HTTP endpoints for MCP communication (/mcp, /health, /status)
- [x] Implement STDIO transport over HTTP bridge

## MCP Tools Implementation
- [x] Verify existing persistent file tools are working with HTTP transport
- [x] Test get_persistent_files tool functionality
- [x] Test add_persistent_files tool functionality
- [x] Test remove_persistent_files tool functionality
- [x] Test clear_persistent_files tool functionality

## Configuration and Settings
- [x] Add server port configuration option
- [x] Implement default port selection (with fallback if port is occupied)
- [x] Add server status monitoring
- [x] Add user-configurable server settings in plugin settings UI
- [x] Allow users to configure server port in settings
- [x] Add option to enable/disable MCP server

## Error Handling and Logging
- [x] Add comprehensive error handling for server startup failures
- [x] Implement proper logging for server operations
- [x] Handle port conflicts gracefully
- [x] Add proper exception handling for MCP request processing


## Tool Window Integration
- [ ] Create tool window to display server status
- [ ] Show list of available MCP tools in tool window
- [ ] Add controls to start/stop server from tool window
