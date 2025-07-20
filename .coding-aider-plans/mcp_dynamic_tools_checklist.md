# [Coding Aider Plan - Checklist] Dynamic MCP Tool System

## Phase 1: Core Infrastructure

- [x] Create `McpTool` interface with methods: `getName()`, `getDescription()`, `getInputSchema()`, `execute()`
- [x] Create `McpToolMetadata` data class for tool information
- [x] Create `McpToolRegistry` service for tool discovery and management
- [x] Add tool registry to dependency injection configuration
- [x] Create base exception classes for tool errors

## Phase 2: Tool Implementation

- [x] Create `GetPersistentFilesTool` class implementing `McpTool`
- [x] Create `AddPersistentFilesTool` class implementing `McpTool`
- [x] Create `RemovePersistentFilesTool` class implementing `McpTool`
- [x] Create `ClearPersistentFilesTool` class implementing `McpTool`
- [x] Register all tool implementations as IntelliJ services
- [ ] Add tool enable/disable configuration to settings

## Phase 3: Service Integration

- [x] Update `McpServerService` to inject `McpToolRegistry`
- [x] Replace hard-coded tool registration with dynamic discovery
- [x] Remove individual tool enable/disable flags
- [x] Update tool configuration methods to use registry
- [x] Remove hard-coded tool methods from `McpServerService`
- [x] Update server startup logging to show discovered tools
- [x] Fix method compatibility issues between registry and service
- [x] Add tool count information to server status endpoint

## Phase 4: UI Updates

- [x] Update `McpServerToolWindow` to inject `McpToolRegistry`
- [x] Replace hard-coded checkboxes with dynamic tool list
- [x] Implement dynamic checkbox generation for discovered tools
- [x] Update tool configuration persistence
- [x] Update connection information display
- [x] Add tool count and status to server information

## Phase 6: Cleanup and Validation

- [x] Remove unused imports and methods
- [x] Fix method compatibility issues in McpToolRegistry
- [ ] Verify all existing functionality still works
- [ ] Test MCP client connections with new tool system
- [ ] Validate tool metadata and schemas
- [ ] Check error handling for tool failures
- [ ] Verify settings persistence works correctly
