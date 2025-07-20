# [Coding Aider Plan] Dynamic MCP Tool System

## Overview

This plan outlines the implementation of a dynamic MCP tool system that replaces the current hard-coded tools with a flexible, extensible architecture using IntelliJ's dependency injection framework. The system will allow easy addition of new MCP tools through a common interface and automatic discovery mechanism.

## Problem Description

Currently, the MCP server implementation has several limitations:

1. **Hard-coded Tools**: All MCP tools (get_persistent_files, add_persistent_files, remove_persistent_files, clear_persistent_files) are hard-coded in the `McpServerService` class
2. **No Extensibility**: Adding new tools requires modifying the core service class
3. **Manual Registration**: Tools must be manually registered in the `addPersistentFileTools()` method
4. **Static Configuration**: Tool enable/disable functionality is handled through individual boolean flags
5. **Tight Coupling**: Tool logic is directly embedded in the service class, making it difficult to test and maintain

## Goals

### Primary Goals

1. **Create Tool Interface**: Define a common interface that all MCP tools must implement
2. **Implement Dependency Injection**: Use IntelliJ's service framework to automatically discover and inject tool implementations
3. **Dynamic Tool Discovery**: Automatically discover and register all available tools without manual configuration
4. **Refactor Existing Tools**: Convert current hard-coded tools to use the new interface
5. **Update Tool Window**: Modify the MCP tool window to dynamically handle tool configuration based on discovered tools

### Secondary Goals

1. **Better Separation of Concerns**: Separate tool logic from server management
2. **Enhanced Configurability**: Allow per-tool enable/disable functionality through a unified mechanism

## Implementation Strategy

### Phase 1: Core Infrastructure
- Create `McpTool` interface with standard methods
- Create `McpToolRegistry` service for tool discovery and management
- Define tool metadata structure for configuration

### Phase 2: Tool Refactoring
- Convert existing persistent file tools to implement the new interface
- Create individual tool classes for each current tool
- Remove hard-coded tool logic from `McpServerService`

### Phase 3: Service Integration
- Update `McpServerService` to use the tool registry
- Implement dynamic tool registration in the MCP server
- Update tool configuration management

### Phase 4: UI Updates
- Modify `McpServerToolWindow` to dynamically display available tools
- Implement dynamic checkbox generation for tool enable/disable
- Update status and configuration display

## Additional Notes and Constraints

### Technical Constraints
- Must maintain backward compatibility with existing MCP clients
- Should not break existing persistent file functionality
- Must work with IntelliJ's service lifecycle management
- Should handle tool initialization errors gracefully

### Design Considerations
- Tools should be stateless where possible
- Tool configuration should be persisted in settings
- Error handling should be consistent across all tools
- Tool metadata should include name, description, and schema information

### Performance Considerations
- Tool discovery should happen at startup, not on every request
- Tool registry should cache tool instances
- Configuration changes should not require server restart where possible

## References

- [IntelliJ Platform SDK - Services](https://plugins.jetbrains.com/docs/intellij/plugin-services.html)
- [MCP Kotlin SDK Documentation](docs/mcp-sdk-docs/USER_GUIDE.md)
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
