# [Coding Aider Plan - Checklist] Dynamic MCP Tool System

## Phase 1: Core Infrastructure

- [ ] Create `McpTool` interface with methods: `getName()`, `getDescription()`, `getInputSchema()`, `execute()`
- [ ] Create `McpToolMetadata` data class for tool information
- [ ] Create `McpToolRegistry` service for tool discovery and management
- [ ] Add tool registry to dependency injection configuration
- [ ] Create base exception classes for tool errors

## Phase 2: Tool Implementation

- [ ] Create `GetPersistentFilesTool` class implementing `McpTool`
- [ ] Create `AddPersistentFilesTool` class implementing `McpTool`
- [ ] Create `RemovePersistentFilesTool` class implementing `McpTool`
- [ ] Create `ClearPersistentFilesTool` class implementing `McpTool`
- [ ] Register all tool implementations as IntelliJ services
- [ ] Add tool enable/disable configuration to settings

## Phase 3: Service Integration

- [ ] Update `McpServerService` to inject `McpToolRegistry`
- [ ] Replace hard-coded tool registration with dynamic discovery
- [ ] Remove individual tool enable/disable flags
- [ ] Update tool configuration methods to use registry
- [ ] Remove hard-coded tool methods from `McpServerService`
- [ ] Update server startup logging to show discovered tools

## Phase 4: UI Updates

- [ ] Update `McpServerToolWindow` to inject `McpToolRegistry`
- [ ] Replace hard-coded checkboxes with dynamic tool list
- [ ] Implement dynamic checkbox generation for discovered tools
- [ ] Update tool configuration persistence
- [ ] Update connection information display
- [ ] Add tool count and status to server information

## Phase 6: Cleanup and Validation

- [ ] Remove unused imports and methods
- [ ] Verify all existing functionality still works
- [ ] Test MCP client connections with new tool system
- [ ] Validate tool metadata and schemas
- [ ] Check error handling for tool failures
- [ ] Verify settings persistence works correctly
